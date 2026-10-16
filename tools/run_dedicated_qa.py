"""Disposable 26.2 dedicated server, bound strictly to this computer's loopback.

Reads installed libraries/API only. No firewall changes, LAN listener, tunnels,
normal saves, accounts, launcher configuration or server runtime in release JARs.
"""
import argparse
import hashlib
import json
import shutil
import socket
import subprocess
import time
from pathlib import Path
from zipfile import ZipFile

ROOT = Path(__file__).resolve().parents[1]
JAVA = Path.home() / 'AppData/Roaming/.minecraft/runtime/java-runtime-epsilon'
INSTANCE = Path.home() / 'AppData/Roaming/.minecraft/versions/fo262'
MC = INSTANCE.parent.parent
VERSION = next(v.split('=', 1)[1] for v in (ROOT/'gradle.properties').read_text().splitlines() if v.startswith('mod_version='))


def sha(data):
    return hashlib.sha256(data).hexdigest()


def copy_mod(game, source):
    assert source.is_file(), source
    target = game/'mods'/source.name
    shutil.copy2(source, target)
    assert sha(source.read_bytes()) == sha(target.read_bytes())


def metadata_libraries():
    metadata = json.loads((INSTANCE/'fo262.json').read_text('utf-8-sig'))
    libs = []
    for lib in metadata['libraries']:
        group, artifact, version, *classifier = lib['name'].split(':')
        path = lib.get('downloads', {}).get('artifact', {}).get('path')
        if not path:
            path = f'{group.replace(".", "/")}/{artifact}/{version}/{artifact}-{version}'+('-'+classifier[0] if classifier else '')+'.jar'
        if group == 'net.fabricmc' or group == 'org.ow2.asm':
            full = MC/'libraries'/path
            assert full.is_file(), full
            libs.append(full)
    return metadata, libs


def prepare_server(game, port, with_qa):
    assert game.parent == (ROOT/'build/dedicated-qa').resolve() and not game.exists()
    (game/'mods').mkdir(parents=True)
    temp = game/'temp'; temp.mkdir()
    bundle = ROOT.parent/'.gradle-user/caches/fabric-loom/26.2/minecraft-server.jar'
    cp = []
    with ZipFile(bundle) as archive:
        for kind in ('versions', 'libraries'):
            for row in archive.read(f'META-INF/{kind}.list').decode().splitlines():
                checksum, _, relative = row.split('\t')
                target = (game/kind/relative).resolve()
                assert game in target.parents
                data = archive.read(f'META-INF/{kind}/{relative}')
                assert sha(data) == checksum
                target.parent.mkdir(parents=True, exist_ok=True); target.write_bytes(data)
                cp.append(target)
    _, fabric = metadata_libraries(); cp.extend(fabric)
    # All client-only Minecraft classes are absent from this server classpath.
    assert not any('client' in p.name or 'lwjgl' in p.name for p in cp)
    release = ROOT/f'build/releases/{VERSION}/guogaology-dimension-26.2-{VERSION}.jar'
    copy_mod(game, release)
    found = False
    for jar in (INSTANCE/'mods').glob('*.jar'):
        try:
            with ZipFile(jar) as archive:
                if 'fabric.mod.json' not in archive.namelist(): continue
                identity = json.loads(archive.read('fabric.mod.json'))['id']
        except (KeyError, json.JSONDecodeError):
            continue
        if identity == 'fabric-api': copy_mod(game, jar); found = True
    assert found, 'Matching installed Fabric API is missing'
    if with_qa:
        copy_mod(game, ROOT/f'ports/26.2/build/qa/guogaology-merge-qa-{VERSION}.jar')
    (game/'eula.txt').write_text('eula=true\n', encoding='utf8')
    (game/'server.properties').write_text(
        'server-ip=127.0.0.1\n'+f'server-port={port}\n'+
        'online-mode=false\nenforce-secure-profile=false\nenable-status=false\n'
        'enable-query=false\nenable-rcon=false\nenable-jmx-monitoring=false\n'
        'management-server-enabled=false\nwhite-list=false\nmax-players=2\n'
        'allow-flight=true\nview-distance=4\nsimulation-distance=4\n'
        'spawn-protection=0\npause-when-empty-seconds=0\nlevel-name=world\n'
        'level-seed=5542262\nmotd=Local loopback QA only\n', encoding='utf8')
    args = ['-Xms1G', '-Xmx5G', '--enable-native-access=ALL-UNNAMED', '-Djava.awt.headless=true',
            '-Dfile.encoding=UTF-8', '-Dfabric.development=false', f'-Djava.io.tmpdir={temp}',
            f'-Djna.tmpdir={temp}', f'-Dguogaology.qa.dedicated055={str(with_qa).lower()}',
            '-cp', ';'.join(map(str, cp)), 'net.fabricmc.loader.impl.launch.knot.KnotServer', 'nogui']
    argfile = game/'server.args'
    argfile.write_text('\n'.join('"'+str(x).replace('\\', '/').replace('"', '\\"')+'"' for x in args), 'utf8')
    return [str(JAVA/'bin/java.exe'), '@'+str(argfile)]


def text(path):
    return path.read_text('utf8', errors='replace') if path.exists() else ''


def wait_until(process, predicate, timeout, label):
    deadline = time.monotonic()+timeout
    while time.monotonic() < deadline:
        if predicate(): return
        if process.poll() is not None: raise RuntimeError(f'{label}: exited {process.returncode}')
        time.sleep(.25)
    raise TimeoutError(label)


def loopback_listeners(process, port):
    rows = [row.split() for row in subprocess.check_output(['netstat', '-ano', '-p', 'tcp'], text=True).splitlines()
            if row.split() and row.split()[-1] == str(process.pid) and 'LISTENING' in row]
    assert rows and all(row[1] == f'127.0.0.1:{port}' for row in rows), rows
    return rows


def run(a):
    started = time.monotonic()
    game = (ROOT/'build/dedicated-qa'/a.label).resolve()
    with socket.socket() as probe:
        probe.bind(('127.0.0.1', 0)); port = probe.getsockname()[1]
    command = prepare_server(game, port, a.with_qa)
    logpath = game/'server-output.log'
    report = {'version': VERSION, 'minecraft': '26.2', 'bind': '127.0.0.1', 'port': port,
              'serverJar': f'guogaology-dimension-26.2-{VERSION}.jar', 'serverClientClassesPresent': False}
    print(f'Loopback-only dedicated QA: {game}, 127.0.0.1:{port}', flush=True)
    with logpath.open('w', encoding='utf8') as log:
        server = subprocess.Popen(command, cwd=game, stdin=subprocess.PIPE, stdout=log,
                                  stderr=subprocess.STDOUT, creationflags=subprocess.CREATE_NO_WINDOW)
        report['serverPid'] = server.pid
        (game/'run.json').write_text(json.dumps(report, indent=2), 'utf8')
        try:
            wait_until(server, lambda: ')! For help, type "help"' in text(logpath), 240, 'dedicated startup')
            assert f'Starting Minecraft server on 127.0.0.1:{port}' in text(logpath), 'Listener is not strictly loopback'
            report['listeners'] = loopback_listeners(server, port)
            report['startup'] = 'passed'
            if a.with_qa:
                run_network_client(game, port, a)
            else:
                for value in ('list', 'save-all flush'):
                    server.stdin.write((value+'\n').encode()); server.stdin.flush(); time.sleep(1)
            server.stdin.write(b'stop\n'); server.stdin.flush()
            code = server.wait(timeout=90)
            assert code == 0 and (game/'world/level.dat').is_file(), 'Dedicated shutdown/save failed'
            assert 'All dimensions are saved' in text(logpath), 'Missing complete save marker'
            report['shutdown'] = 'passed'
            if a.with_qa:
                report['mechanics'] = text(game/'dedicated-mechanics-ok.txt').strip()
                with logpath.open('a', encoding='utf8') as reload_log:
                    server = subprocess.Popen(command, cwd=game, stdin=subprocess.PIPE, stdout=reload_log,
                                              stderr=subprocess.STDOUT, creationflags=subprocess.CREATE_NO_WINDOW)
                    wait_until(server, lambda: text(logpath).count(')! For help, type "help"') >= 2, 180, 'saved-world restart')
                    report['reloadListeners'] = loopback_listeners(server, port)
                    run_network_client(game, port, a, reload=True)
                    server.stdin.write(b'stop\n'); server.stdin.flush(); assert server.wait(timeout=90) == 0
                assert (game/'dedicated-reload-ok.txt').is_file()
                report['reload'] = text(game/'dedicated-reload-ok.txt').strip()
            report['productionSha256'] = sha((game/'mods'/report['serverJar']).read_bytes())
            with socket.socket() as probe:
                probe.settimeout(1)
                assert probe.connect_ex(('127.0.0.1', port)) != 0, 'Test port is still listening after shutdown'
            report['portClosedAfterShutdown'] = True
            report['elapsedSeconds'] = round(time.monotonic()-started, 2)
            (game/'dedicated-ok.json').write_text(json.dumps(report, indent=2), 'utf8')
            print(json.dumps(report, indent=2), flush=True)
        except BaseException:
            print(text(logpath)[-8000:], flush=True)
            raise
        finally:
            if server.poll() is None:
                try: server.stdin.write(b'stop\n'); server.stdin.flush(); server.wait(timeout=45)
                except (OSError, subprocess.TimeoutExpired): server.terminate(); server.wait(timeout=20)


def run_network_client(game, port, a, reload=False):
    from run_merge_qa import allowed
    client = game/('client-reload' if reload else 'client')
    assert not client.exists(); (client/'mods').mkdir(parents=True)
    metadata, _ = metadata_libraries(); cp = []
    for lib in metadata['libraries']:
        if not allowed(lib): continue
        path = lib.get('downloads', {}).get('artifact', {}).get('path')
        if not path:
            group, artifact, version, *classifier = lib['name'].split(':')
            path = f'{group.replace(".", "/")}/{artifact}/{version}/{artifact}-{version}'+('-'+classifier[0] if classifier else '')+'.jar'
        file = MC/'libraries'/path; assert file.is_file(), file; cp.append(file)
    cp.append(INSTANCE/'fo262.jar')
    for jar in (game/'mods').glob('*.jar'): copy_mod(client, jar)
    # The optional renderer stays in the client. It never enters server mods/classpath.
    for jar in (INSTANCE/'mods').glob('*.jar'):
        try:
            with ZipFile(jar) as archive: identity = json.loads(archive.read('fabric.mod.json'))['id']
        except (KeyError, json.JSONDecodeError): continue
        if identity == 'sodium': copy_mod(client, jar)
    temp = client/'temp'; temp.mkdir()
    (client/'options.txt').write_text('pauseOnLostFocus:false\nfullscreen:false\nrenderDistance:4\nsimulationDistance:5\nsoundCategory_master:0.0\nlang:zh_cn\nguiScale:4\n', 'utf8')
    args = ['-Xmx4G', '--enable-native-access=ALL-UNNAMED', '-Dfile.encoding=UTF-8', '-Dfabric.development=false',
            '-Dguogaology.qa.dedicated055=true', f'-Dguogaology.qa.address=127.0.0.1:{port}',
            f'-Djava.io.tmpdir={temp}', f'-Djna.tmpdir={temp}', f'-Dorg.lwjgl.system.SharedLibraryExtractPath={temp}',
            '-cp', ';'.join(map(str, cp)), metadata['mainClass'], '--username', 'GuogaologyQA', '--version', '26.2',
            '--gameDir', str(client), '--assetsDir', str(MC/'assets'), '--assetIndex', metadata['assetIndex']['id'],
            '--uuid', '00000000000000000000000000000001', '--accessToken', '0', '--userType', 'legacy',
            '--versionType', 'release', '--width', '1600', '--height', '1200']
    argfile = client/'client.args'
    argfile.write_text('\n'.join('"'+str(x).replace('\\', '/').replace('"', '\\"')+'"' for x in args), 'utf8')
    logfile = client/'client-output.log'
    with logfile.open('w', encoding='utf8') as log:
        process = subprocess.Popen([str(JAVA/'bin/java.exe'), '@'+str(argfile)], cwd=client, stdout=log,
                                   stderr=subprocess.STDOUT, creationflags=subprocess.CREATE_NO_WINDOW)
        try: code = process.wait(timeout=840)
        except subprocess.TimeoutExpired: raise RuntimeError('Dedicated network client timeout')
        finally:
            if process.poll() is None:
                process.terminate(); process.wait(timeout=20)
    marker = client/('dedicated-client-reload-ok.txt' if reload else 'dedicated-client-ok.txt')
    if code or not marker.is_file():
        print(text(logfile)[-10000:]); raise RuntimeError(f'Dedicated network client failed {code}')
    print(text(marker).strip(), flush=True)


if __name__ == '__main__':
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--label', required=True)
    parser.add_argument('--with-qa', action='store_true')
    run(parser.parse_args())

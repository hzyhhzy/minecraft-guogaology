"""Compare exact069 and candidate0610 visual scenes in fresh hidden263 worlds."""
from pathlib import Path
import json,os,shutil,subprocess
from zipfile import ZipFile
import run_release_263_qa as base
ROOT=base.ROOT
ENTRY='dev.guogaology.releaseqa263.Visual0610Runtime'
OLD_SHA='c9ce1751caaeb5fecc448de2c5003d9d1ec51ddbd9ae6e07004c65f3671bc92e'
def run(args):
    base.require(not(args.startup_control or args.general_audit or args.repair069 or args.voxy),'Dedicated visual QA only')
    game,instance,before,current,command=base.prepare(args)
    release='0.6.9' if args.baseline069 else current
    try:
        mods=game/'mods';staging=base.read_json(game/'staging.json')
        mains=[p for p in mods.glob('*.jar') if base.mod_metadata(p).get('id')=='guogaology']
        helpers=[p for p in mods.glob('*.jar') if base.mod_metadata(p).get('id')==base.QA_ID]
        base.require(len(mains)==len(helpers)==1,'Unique production and QA inputs')
        main=mains[0]
        if args.baseline069:
            old=ROOT/'build/releases/0.6.9/guogaology-dimension-26.3-0.6.9.jar'
            base.validate_mod(old,'guogaology','0.6.9')
            base.require(base.sha256(old)==OLD_SHA,'Exact published069 required')
            base.require(main.parent==mods and main.resolve().is_relative_to((ROOT/'build/runtime-qa263').resolve()),'Only disposable staged main replacement')
            main.unlink();main=mods/old.name;shutil.copyfile(old,main)
            for row in staging['mods']:
                if row['id']=='guogaology':row.update(file=main.name,version=release,source=str(old),sourceSha256=OLD_SHA,sha256=base.sha256(main))
        helper=helpers[0];source=ROOT/'ports/26.3/build/qa'/helper.name
        info=base.mod_metadata(source);info['entrypoints']={'client':[ENTRY]};info['depends']['guogaology']='='+release
        with ZipFile(source) as src:
            base.require(ENTRY.replace('.','/')+'.class' in src.namelist(),'Build visual helper first')
            with ZipFile(helper,'w') as dst:
                for item in src.infolist():dst.writestr(item,json.dumps(info,ensure_ascii=False,indent=2).encode() if item.filename=='fabric.mod.json' else src.read(item.filename))
        for row in staging['mods']:
            if row['id']==base.QA_ID:row.update(sha256=base.sha256(helper),disposableEntrypoint=ENTRY,productionDependency=release)
        command=[('-Dguogaology.qa263.release='+release) if x.startswith('-Dguogaology.qa263.release=') else x for x in command]
        command.insert(1,'-Dguogaology.visual0610=true')
        staging.update(release=release,visualBaseline069=args.baseline069,command=command)
        (game/'staging.json').write_text(json.dumps(staging,ensure_ascii=False,indent=2)+'\n',encoding='utf-8')
        base.write_json_new(game/'visual0610-inputs.json',{'productionVersion':release,'productionJar':main.name,'productionSha256':base.sha256(main),'helperJar':helper.name,'helperSourceSha256':base.sha256(source),'helperStagedSha256':base.sha256(helper),'baseline069':args.baseline069,'sodium':not args.without_sodium})
        env=os.environ.copy()
        for key in ('JAVA_TOOL_OPTIONS','JDK_JAVA_OPTIONS','_JAVA_OPTIONS'):env.pop(key,None)
        env.update(TEMP=str(game/'temp'),TMP=str(game/'temp'))
        with (game/'launch-output.log').open('x',encoding='utf-8') as output:
            proc=subprocess.Popen(command,cwd=game,env=env,stdout=output,stderr=subprocess.STDOUT,creationflags=subprocess.CREATE_NO_WINDOW)
            base.write_json_new(game/'qa-process.json',{'pid':proc.pid,'gameDirectory':str(game),'mode':'visual0610','minecraft':'26.3'})
            try:code=proc.wait(timeout=args.timeout)
            except subprocess.TimeoutExpired:
                proc.terminate()
                try:proc.wait(timeout=15)
                except subprocess.TimeoutExpired:proc.kill();proc.wait(timeout=15)
                raise RuntimeError('Own disposable visual JVM timed out')
        base.require(code==0 and (game/'visual0610-ok.txt').is_file(),'Visual runtime failed: '+str(game/'launch-output.log'))
        result=base.read_json(game/'visual0610-results.json')
        base.require(result.get('success') is True,'Visual assertions failed')
        base.write_json_new(game/'visual0610-runner-ok.json',{'success':True,'release':release,'exitCode':code,'baseline069':args.baseline069})
        print('VISUAL0610_NATIVE_OK '+str(game),flush=True)
    finally:base.verify_normal_unchanged(game,instance,before)
def main():
    parser=base.parser();parser.add_argument('--baseline069',action='store_true')
    run(parser.parse_args())
if __name__=='__main__':main()

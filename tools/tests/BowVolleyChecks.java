import dev.guogaology.mining.*;
import java.util.UUID;

/** Regression checks for cross-arrow/save-reload budgets, distinct targets and launch snapshots. */
public final class BowVolleyChecks {
    static void check(boolean value,String why){if(!value)throw new AssertionError(why);}
    public static void main(String[] args){
        String id=UUID.randomUUID().toString();UUID a=UUID.randomUUID(),b=UUID.randomUUID(),c=UUID.randomUUID(),first=UUID.randomUUID(),second=UUID.randomUUID();
        var data=new BowVolleyData();data.register(id,a);data.register(id,b);data.register(id,c);
        check(data.claimDirect(id,first),"first arrow hit");check(!data.claimDirect(id,first),"overlapping arrow cannot repeat direct hit");
        check(data.claimBurst(id,first),"direct target also receives one burst");check(!data.claimBurst(id,first),"overlapping bursts cannot repeat");
        check(data.claimDirect(id,second),"piercing may hit a different target");
        data.destroy(id,a);data=BowVolleyData.load(data.save());
        check(data.directSeen(id,first)&&data.directSeen(id,second)&&data.burstSeen(id,first),"ledger survives different-arrow unload and reload");
        data.destroy(id,b);check(data.directSeen(id,first),"one live arrow retains budget");data.destroy(id,c);check(!data.directSeen(id,first)&&data.save().equals("{}"),"last arrow removes ledger entry");
        var shot=new BowShotData(id,UUID.randomUUID().toString(),"team",4,1,.25,.3,3,5);shot.burstUsed=true;
        var saved=BowShotData.load(shot.save());check(saved!=null&&saved.burstUsed&&saved.guogao==3&&saved.controlSeconds==5,"per-arrow first-burst and full numeric snapshot persist");
        var arrow=saved.copy();var side=saved.copy();check(!arrow.burstUsed&&arrow.livingHits==0&&side.livingHits==0,"launch copies reset independent arrow budgets");
        check(arrow.recordLivingHit(),"first living hit");arrow.burstUsed=true;arrow=BowShotData.load(arrow.save());
        check(arrow!=null&&arrow.livingHits==1&&arrow.canHitLiving()&&arrow.burstUsed,"reload retains one hit and consumed first burst");
        check(arrow.recordLivingHit(),"second living hit after reload");arrow=BowShotData.load(arrow.save());
        check(arrow!=null&&arrow.livingHits==2&&!arrow.canHitLiving()&&!arrow.recordLivingHit(),"reload cannot reset a used two-target piercing budget");
        check(side.livingHits==0&&side.canHitLiving()&&!side.burstUsed,"a side arrow keeps its own unused budget");
        var next=arrow.copy();check(next.livingHits==0&&next.canHitLiving()&&!next.burstUsed,"a new launch copy resets both used budgets");
        check(Math.abs(saved.damage(8)-9)<1e-6,"normal fixed HP is charge-scaled and not multiplied by arrow speed");
        var deep=new BowShotData(id,shot.owner,"",0,2.5,.25,.3,0,0);check(deep.damage(8)==20,"deep multiplier applies once to effective vanilla damage");
        check(BowShotData.load("")==null&&BowShotData.load("{}")==null,"ordinary arrows have no snapshot");
        System.out.println("Bow snapshot and volley persistence checks passed");
    }
}

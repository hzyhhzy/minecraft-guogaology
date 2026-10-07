package dev.guogaology.client;

import org.joml.Matrix4f;

/** Reload-time, API-independent zero-phase pose for the cached distant body. */
final class CoreStaticPose {
    private CoreStaticPose(){}
    static Matrix4f matrix(String motion,int part,float motionScale){
        var pose=new Matrix4f().translation(.5f,.5f,.5f);
        if(motion.equals("absence_orbits")&&part<10){
            int component=(part-1)%3,nest=(part-1)/3;float offset=nest*29;
            if(component==0)pose.rotateX(radians(offset)).rotateZ(radians((float)Math.sin(nest)*17));
            else if(component==1)pose.rotateZ(radians(-offset)).rotateY(radians((float)Math.sin(nest)*23));
            else pose.rotateY(radians(offset+17)).rotateZ(radians((float)Math.sin(nest)*26));
        }else if(motion.equals("nested")&&part>1)pose.rotateX(radians((float)Math.sin(part)*14));
        float scale=part>=10?1:motionScale;
        return pose.scale(scale).translate(-.5f,-.5f,-.5f);
    }
    private static float radians(float degrees){return (float)Math.toRadians(degrees);}
}

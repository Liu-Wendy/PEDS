package MPC.Solver;

import MPC.Location;
import MPC.Transition;

import java.util.Map;
import java.util.Objects;

public class ToTerm {


    static public Term stateTerm(ListOperationContext c, int aut_id, Location st, int step) {
        String var = "st_" + aut_id + "_" + st.getNo() + "_" + step + "_" + st.name;
        return c.getBoolTerm(var);
    }

    static public Term transitionTerm(ListOperationContext c, int aut_id, Transition tr, int step) {
        if(Objects.equals(tr.label, "stutter")){
            return stutterTerm(c,aut_id,step);
        }
        String var = "tr_" + aut_id + "_" + tr.id + "_" + step + "_" + tr.label;
//        System.out.println(var);
        return c.getBoolTerm(var);
    }

    static public Term stutterTerm(ListOperationContext c,int aut_id,int step){
        String var="stutter_"+aut_id+"_-1_"+step+"_stutter";
        return c.getBoolTerm(var);
    }

}

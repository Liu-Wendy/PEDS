package MPC.Solver;

import MPC.Location;
import MPC.Transition;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class ALocation {
    // ComposedAcyclicAutomata* m_composed_automata;
    public int		m_ID;
    public int		m_automaton_ID;
    public Location m_loction;
    public int		m_depth;
    public int		m_occs; //number of occurrences before
    public int		m_max_occs;
    public int		m_max_length_of_paths; //the maximum length of path which has this state as the target

    public ArrayList<ATransition> m_fromTrans=new ArrayList<>();
    public ArrayList<ATransition> m_nextTrans=new ArrayList<>();

    public Map<Location,Integer> m_state_occs=new HashMap<>();

    public ALocation(int id, int aut_id, Location st, int depth, int max_nodes, boolean setTrans){
        m_ID=id;
        m_automaton_ID=aut_id;
        m_loction=st;
        m_depth=depth;
        m_occs=-1;
        m_max_occs=-1;
        m_max_length_of_paths=-1;
        if (setTrans)
            setTransitions();
    }
    void inherit(ALocation previous, ATransition fromTranPtr){

    }
    void linkFrom(ALocation previous, ATransition fromTranPtr){
        updateStateOccs(previous);
        m_fromTrans.add(fromTranPtr);
    }
    void updateStateOccs(ALocation previous){

    }
    void setTransitions(){
        ArrayList<Transition> tranList=m_loction.next_trans;
        for(Transition tr:tranList){
            ATransition aTransition=new ATransition(m_automaton_ID,tr,m_depth);
            aTransition.setSrc(this);
            this.m_nextTrans.add(aTransition);
        }
    }

    Location toState(int id){
        Location rtn=new Location(id,m_loction.name+"@"+Integer.toString(m_depth));
        rtn.flows=m_loction.flows;
        rtn.invariants=m_loction.invariants;
//        rtn.next_trans=m_loction.next_trans;
        return rtn;
    }


}

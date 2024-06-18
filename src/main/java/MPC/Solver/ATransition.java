package MPC.Solver;

import MPC.Location;
import MPC.Transition;

import java.util.ArrayList;
import java.util.HashMap;

public class ATransition {
    public int m_automaton_ID;
    public ALocation m_src;
    public ALocation m_dst;
    public Transition m_transition;
    public int m_max_occs;
    public int occs;

    public ATransition(int AcyclicAutomaton_ID, Transition tran,int occs) {
        m_automaton_ID = AcyclicAutomaton_ID;
        m_transition = tran;
        this.occs=occs;
    }


    public ATransition(int AcyclicAutomaton_ID, Transition tran) {
        m_automaton_ID = AcyclicAutomaton_ID;
        m_transition = tran;
    }

    public void setSrc(ALocation m_src) {
        this.m_src = m_src;
    }

    public void setDst(ALocation m_dst) {
        this.m_dst = m_dst;
    }

    public Transition toTransition(Location src, Location dst) {
        Transition rtn = new Transition(src.getNo(), dst.getNo());
        rtn.label = m_transition.label;
        rtn.shared = m_transition.shared;
        rtn.guards = m_transition.guards;
        rtn.assignments = m_transition.assignments;
        return rtn;
    }

    public Transition toTransition(int id, Location src, Location dst) {
        Transition rtn = new Transition(src.getNo(), dst.getNo());
        rtn.id=id;
        rtn.label = m_transition.label+"@"+occs;
        rtn.shared = m_transition.shared;
        rtn.guards = m_transition.guards;
        rtn.assignments = m_transition.assignments;
//        rtn.next_trans=dst.next_trans;
        return rtn;
    }
}

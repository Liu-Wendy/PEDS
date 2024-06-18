package MPC.Solver;

import MPC.Automata;
import MPC.Location;
import MPC.Transition;

import java.util.*;

public class AcyclicAutomaton {
    public int m_ID;
    public Automata m_automata;
    public ALocation m_initialLocation;
    public ALocation m_targetLocation;

    public Map<Location, Map<Integer, ALocation>> m_state_map = new HashMap<>();
    public Map<Location, ArrayList<ALocation>> m_state_list_map = new HashMap<>();

    public ArrayList<ALocation> m_state_list = new ArrayList<>();
    public ArrayList<ATransition> m_tran_list = new ArrayList<>();

    public Map<Integer, ALocation> id_state_map = new HashMap<>();
    public Map<Location, Integer> least_step_to_target = new HashMap<>();

    int MAX_DEPTH;
    int max_nr_nodes;
    int max_length_of_paths;
    int nr_nodes;

    ArrayList<ALocation> m_topologic_list;

    public AcyclicAutomaton(Automata aut, int MAX_DEPTH) {
        m_ID = aut.ID;
        m_automata = aut;
        nr_nodes = 0;
        this.MAX_DEPTH = MAX_DEPTH;
        max_nr_nodes = m_automata.getLocationSize() * (MAX_DEPTH + 1);
        m_initialLocation = new ALocation(nr_nodes++, m_ID, m_automata.getInitLocation(), 0, max_nr_nodes, true);
        m_initialLocation.m_occs = 0;
        m_initialLocation.m_state_occs.put(m_initialLocation.m_loction, 0);
        insertState(m_initialLocation);

        Queue<ALocation> loc_que = new ArrayDeque<>();
        loc_que.add(m_initialLocation);
        while (!loc_que.isEmpty()) {
            ALocation pre = loc_que.poll();
            if (pre.m_depth == MAX_DEPTH) continue;

            int preDepth = pre.m_depth;

            for (ATransition aTr : pre.m_nextTrans) {
                Transition tr = aTr.m_transition;
                Location to = m_automata.locations.get(tr.target);
                int to_occs = preDepth + 1;
                if(!m_tran_list.contains(aTr)) m_tran_list.add(aTr);

                ALocation aLoc = findState(to, to_occs);
                if (aLoc != null) {
                    aTr.m_dst = aLoc;
                    aLoc.linkFrom(pre, aTr);
                } else {
                    aLoc = new ALocation(nr_nodes++, m_ID, to, preDepth + 1, max_nr_nodes, preDepth + 1 < MAX_DEPTH);
                    aLoc.m_occs = to_occs;
                    aTr.m_dst = aLoc;
                    insertState(aLoc);
                    loc_que.add(aLoc);
                }
            }
        }

        show();
    }

    public void show() {
        for (Map.Entry<Integer, ALocation> entry : id_state_map.entrySet()) {
            int x = entry.getKey();
            ALocation y = entry.getValue();
//            System.out.println(y.m_loction.getNo()+" "+y.m_depth);
        }
    }

    ALocation findState(Location st, int occs) {
        if (m_state_map.containsKey(st) && m_state_map.get(st).containsKey(occs)) {
            return m_state_map.get(st).get(occs);
        } else return null;
    }

//    ALocation insertAndFindState(Location st, int occs, int depth) {
//
//    }

    boolean insertState(ALocation location) {
        if (m_state_map.containsKey(location.m_loction) && m_state_map.get(location.m_loction).containsKey(location.m_occs))
            return false;
        Map<Integer, ALocation> tmp = m_state_map.getOrDefault(location.m_loction, new HashMap<>());
        tmp.put(location.m_occs, location);
        m_state_map.put(location.m_loction, tmp);

        ArrayList<ALocation> list = m_state_list_map.getOrDefault(location.m_loction, new ArrayList<>());
        list.add(location);
        m_state_list_map.put(location.m_loction, list);

        id_state_map.put(location.m_ID, location);
        m_state_list.add(location);
        return true;
    }

    void eraseState(ALocation st) {

    }

    void eraseRedundantStates(ALocation tgt) {

    }

    void computeLeastStep() {
//        Queue<Location> que=new PriorityQueue<>();
//        que.add(m_automata)
    }

    void build_tran_map() {

    }

    void freeAcyclicAutomaton() {

    }

    int getMaxId() {
        return nr_nodes - 1;
    }

//    ALocation getStateById(int id) {
//
//    }
//
//    ArrayList<ALocation> get_topologic_list() {
//
//    }
//
    public Automata toAutomaton() {
        Automata rtn=new Automata(m_automata.name);
        rtn.ID=m_automata.ID;
        rtn.parameters=m_automata.parameters;
        rtn.shared_labels=new ArrayList<>();
        rtn.shared_transList=new ArrayList<>();
        rtn.rangeParameters=m_automata.rangeParameters;
        rtn.initParameterValues=m_automata.initParameterValues;

        Map<ALocation,Location> aLoc2loc=new HashMap<>();
        int i=0;
        for(ALocation aloc:m_state_list){
            Location newLoc=aloc.toState(i);
            rtn.locations.put(i,newLoc);
            aLoc2loc.put(aloc,newLoc);
            i++;
        }
        Map<ATransition,Transition> aTr2Tr=new HashMap<>();
        for(ATransition aTr:m_tran_list){
            Transition tr=aTr.toTransition(rtn.transitions.size(), aLoc2loc.get(aTr.m_src),aLoc2loc.get(aTr.m_dst));
            rtn.transitions.add(tr);
            aTr2Tr.put(aTr,tr);
        }

        for(ALocation aloc:m_state_list){
            Location loc=aLoc2loc.get(aloc);
            loc.next_trans=new ArrayList<>();
            for(ATransition atr:aloc.m_nextTrans){
                loc.next_trans.add(aTr2Tr.get(atr));
            }
            //为每个节点加上空转换
            Transition tr=new Transition(aloc.m_depth,loc.getNo(),loc.getNo());
            tr.next_trans=loc.next_trans;
            loc.next_trans.add(tr);
            rtn.transitions.add(tr);
        }

        for(Transition tr:rtn.transitions){
            Location dst=rtn.locations.get(tr.target);
            tr.next_trans=dst.next_trans;
            if(tr.shared){
                rtn.shared_labels.add(tr.label);
                rtn.shared_transList.add(tr);
            }
            tr.autId= rtn.ID;
        }

        rtn.initLoc=aLoc2loc.get(m_initialLocation).getNo();
        return rtn;
    }

}

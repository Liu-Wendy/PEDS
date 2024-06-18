package MPC.Solver;

import MPC.Automata;
import MPC.ComposedAutomata;
import MPC.Transition;

import java.util.*;

public class Encoder {
    public ListOperationContext c;
    public SATSolver solver;
    public ComposedAutomata ca;
    public int bound;
    public boolean stutter_enable = true;
    public Map<String,ArrayList<Transition>> allFeasibleTr;
    public Map<String,Set<String>> allFeasibleLabel;
    public Map<String,Set<String>> allInfeasibleLabel;

    public Encoder(ComposedAutomata composedAutomata, SATSolver s, ListOperationContext c, int bound) {
        ca = composedAutomata;
        solver = s;
        this.bound = bound;
        this.c = c;
        allFeasibleTr=new HashMap<>();
        allFeasibleLabel=new HashMap<>();
        allInfeasibleLabel=new HashMap<>();
    }

    public Term encodeTrInitForSingleAut(int autId, int end_step) {
        Term res = new Term(true);
        if (end_step < 0) return res;
        Automata aut = ca.automataList.get(autId);

        Term tmp = new Term(false);
        ArrayList<Transition> transList = aut.locations.get(aut.initLoc).next_trans;
        if (transList.size() <= 1) {
            System.out.println("just one transition for initial location");
            Term x = ToTerm.transitionTerm(c, aut.ID, transList.get(0), 0);
            res.AND(x);
        }
        for (int i = 0; i < transList.size(); i++) {
            Term x = ToTerm.transitionTerm(c, aut.ID, transList.get(i), 0);
            for (int j = i + 1; j < transList.size(); j++) {
                Term y = ToTerm.transitionTerm(c, aut.ID, transList.get(j), 0);
                res.AND(c.makeOr(x.NOT(), y.NOT()));
            }
            tmp.OR(x);
        }
        res.AND(tmp);
        return res;
    }

    public Term encodeTrInit(int end_step) {
        Term res = new Term(true);
        if (end_step < 0) return res;
        for (Automata aut : ca.automataList) {
            Term tmp = new Term(false);
            ArrayList<Transition> transList = aut.locations.get(aut.initLoc).next_trans;
            if (transList.size() <= 1) {
                System.out.println("just one transition for initial location");
                Term x = ToTerm.transitionTerm(c, aut.ID, transList.get(0), 0);
                res.AND(x);
            }
            for (int i = 0; i < transList.size(); i++) {
                Term x = ToTerm.transitionTerm(c, aut.ID, transList.get(i), 0);
                for (int j = i + 1; j < transList.size(); j++) {
                    Term y = ToTerm.transitionTerm(c, aut.ID, transList.get(j), 0);
                    res.AND(c.makeOr(x.NOT(), y.NOT()));
                }
                tmp.OR(x);
            }
            res.AND(tmp);
        }

        return res;
    }

    public Term encodeTrGraphForSingleAut(int id, int end_step) {
        Term res = c.constBoolVal(true);
        if (end_step < 0) return res;
        Automata aut = ca.automataList.get(id);

        ArrayList<Transition> trList = aut.transitions;
        for (int k = 0; k <= end_step; k++) {
            //tmp用于表示至多有一个被选中
            Term tmp = new Term(false);
            for (int m = 0; m < trList.size(); m++) {
                Term x = ToTerm.transitionTerm(c, aut.ID, trList.get(m), k);
                tmp.OR(x);
                for (int n = m + 1; n < trList.size(); n++) {
                    Term y = ToTerm.transitionTerm(c, aut.ID, trList.get(n), k);
                    res.AND(c.makeOr(x.NOT(), y.NOT()));
                }
                //当还没结束时，转换被选中，那么下一个转化一定是当前选中的转化中的后继转化
                if (k <= end_step - 1) {
                    ArrayList<Transition> next_tr_list = trList.get(m).next_trans;
                    Term nextOr = new Term(false);
                    for (Transition transition : next_tr_list) {
                        Term next_tr1 = ToTerm.transitionTerm(c, aut.ID, transition, k + 1);
                        nextOr.OR(next_tr1);
                    }
                    res.AND(c.makeImply(x, nextOr));
                }

            }
            res.AND(tmp);
        }

        return res;

    }

    public Term encodeTrGraph(int end_step) {
        Term res = c.constBoolVal(true);
        if (end_step < 0) return res;

        for (Automata aut : ca.automataList) {
            ArrayList<Transition> trList = aut.transitions;
            for (int k = 0; k <= end_step; k++) {
                //tmp用于表示至多有一个被选中
                Term tmp = new Term(false);
                for (int m = 0; m < trList.size(); m++) {
                    Term x = ToTerm.transitionTerm(c, aut.ID, trList.get(m), k);
                    tmp.OR(x);
                    for (int n = m + 1; n < trList.size(); n++) {
                        Term y = ToTerm.transitionTerm(c, aut.ID, trList.get(n), k);
                        res.AND(c.makeOr(x.NOT(), y.NOT()));
                    }
                    //当还没结束时，转换被选中，那么下一个转化一定是当前选中的转化中的后继转化
                    if (k <= end_step - 1) {
                        ArrayList<Transition> next_tr_list = trList.get(m).next_trans;
                        Term nextOr = new Term(false);
                        for (Transition transition : next_tr_list) {
                            Term next_tr1 = ToTerm.transitionTerm(c, aut.ID, transition, k + 1);
                            nextOr.OR(next_tr1);
                        }
                        res.AND(c.makeImply(x, nextOr));
                    }

                    String label = trList.get(m).label;
                    if (ca.stepSharedLabels.contains(label)) {
                        Term shared = c.constBoolVal(true);
                        Map<Integer, ArrayList<Transition>> sharesTran = ca.find(aut.ID, label, k);
                        for (Map.Entry<Integer, ArrayList<Transition>> entry : sharesTran.entrySet()) {
                            for (Transition transition : entry.getValue()) {
                                Term shared_tr = ToTerm.transitionTerm(c, entry.getKey(), transition, k);
                                shared.AND(shared_tr);
                            }
                        }
                        res.AND(c.makeImply(x, shared));
                    }
                }
                res.AND(tmp);

            }
        }
        return res;

    }

    public Term encodeLabel(int end_step) {
        //对于每个位置
        Term res = c.constBoolVal(true);
        if (end_step <= 0) return res;
        boolean hasFeasibleShareLabel = true;

        for (Automata aut : ca.automataList) {
            ArrayList<Transition> allSharedTrans = aut.shared_transList;
            int allFeasibleSize = 0;
            for (int k = 0; k < end_step; k++) {

                Set<String> infeasibalLabels=allInfeasibleLabel.getOrDefault(Integer.toString(end_step-1),new HashSet<>());
                if (k == 0) {//第一步

                }
                ArrayList<Transition> allFeasibleSharedTrans = new ArrayList<>();
                Set<String> labelSet=new HashSet<>();
                for (Transition tr : allSharedTrans) {
                    String label=tr.label.split("@")[0];
                    if (checkSharedStep(tr, k)) {
                        if(infeasibalLabels.isEmpty()){
                            allFeasibleSharedTrans.add(tr);
                            labelSet.add(label);
                        }
                        if((!infeasibalLabels.isEmpty())){
                            if(infeasibalLabels.contains(label)){
                                Term x = ToTerm.transitionTerm(c, aut.ID, tr, k);
                                res.AND(x.NOT());
                            }else {
                                allFeasibleSharedTrans.add(tr);
                                labelSet.add(label);
                            }
                        }

                    }
                }
                allFeasibleSize += allFeasibleSharedTrans.size();
//                if(allFeasibleSharedTrans.size()==0){
//                    hasFeasibleShareLabel=false;
//                    break;
//                }

//                for (Transition tr : allSharedTrans) {
//                    if(!labelSet.contains(tr.label.split("@")[0])){
//                        for(int n=0;n<ca.automataList.size();n++){
//                            if(n!=aut.ID){
//                                Term x = ToTerm.transitionTerm(c, n, tr, k);
//                                res.AND(x.NOT());
//                            }
//                        }
//                    }
//
//                }

                //第一个里面么有加一组约束
                if (allFeasibleSharedTrans.size() == 0) {
                    Term notSyn = c.constBoolVal(true);
                    for (int m = 0; m < aut.shared_transList.size(); m++) {
                        Term x = ToTerm.transitionTerm(c, aut.ID, aut.shared_transList.get(m), k);
                        notSyn.AND(x.NOT());
                    }
                    res.AND(notSyn);

//                    notSyn = c.constBoolVal(true);
//                    Set<Transition> eachStepST = ca.eachStepSharedTrans.getOrDefault(k,new HashSet<>());
//                    for (Transition tr : eachStepST) {
//                        Term x = ToTerm.transitionTerm(c, tr.autId, tr, k);
//                        notSyn.AND(x.NOT());
//                    }
//                    res.AND(notSyn);
                }


                for (int m = 0; m < allFeasibleSharedTrans.size(); m++) {

                    Term x = ToTerm.transitionTerm(c, aut.ID, allFeasibleSharedTrans.get(m), k);
                    for (int n = m + 1; n < allFeasibleSharedTrans.size(); n++) {
                        Term y = ToTerm.transitionTerm(c, aut.ID, allFeasibleSharedTrans.get(n), k);
                        res.AND(c.makeOr(x.NOT(), y.NOT()));
                    }
                    String label = allFeasibleSharedTrans.get(m).label;
//                    if (ca.stepSharedLabels.contains(label)) {
//                        Term shared = c.constBoolVal(true);
//                        Map<Integer, ArrayList<Transition>> sharesTran = ca.find(aut.ID, label);
//                        for (Map.Entry<Integer, ArrayList<Transition>> entry : sharesTran.entrySet()) {
//                            for (Transition transition : entry.getValue()) {
//                                Term shared_tr = ToTerm.transitionTerm(c, entry.getKey(), transition, k);
//                                shared.AND(shared_tr);
//                            }
//                        }
//                        res.AND(c.makeImply(x, shared));
//                    }


//                    Map<Integer, ArrayList<Transition>> sharesTran = ca.find(aut.ID, label);
//                    if (sharesTran.size() != 0) {
//                        Term shared = c.constBoolVal(true);
//                        for (Map.Entry<Integer, ArrayList<Transition>> entry : sharesTran.entrySet()) {
//                            for (Transition transition : entry.getValue()) {
//                                Term shared_tr = ToTerm.transitionTerm(c, entry.getKey(), transition, k);
//                                shared.AND(shared_tr);
//                            }
//                        }
//                        res.AND(c.makeImply(x, shared));
//                    }
//                    Term tmp = new Term(false);
//                    for (int m = 0; m < trList.size(); m++) {
//                        Term x = ToTerm.transitionTerm(c, aut.ID, trList.get(m), k);
//                        tmp.OR(x);
//                        for (int n = m + 1; n < trList.size(); n++) {
//                            Term y = ToTerm.transitionTerm(c, aut.ID, trList.get(n), k);
//                            res.AND(c.makeOr(x.NOT(), y.NOT()));
//                        }
//                    }
//                    res.AND(tmp);


                    Map<Integer, ArrayList<Transition>> sharesTran = ca.find1(aut.ID, label,k);
                    if (sharesTran.size() != 0) {
                        Term shared = c.constBoolVal(true);
                        for (Map.Entry<Integer, ArrayList<Transition>> entry : sharesTran.entrySet()) {
                            Term shared_or=new Term(false);
                            ArrayList<Transition> trList=entry.getValue();
                            for(int p=0;p<trList.size();p++){
                                Term shared_x = ToTerm.transitionTerm(c, entry.getKey(), trList.get(p), k);
                                shared_or.OR(shared_x);
                                for(int q=p+1;q<trList.size();q++){
                                    Term shared_y = ToTerm.transitionTerm(c, entry.getKey(), trList.get(q), k);
                                    shared.AND(c.makeOr(shared_x.NOT(),shared_y.NOT()));
                                }
                            }

                            shared.AND(shared_or);
                        }
                        res.AND(c.makeImply(x, shared));
                    }
                }
            }
//            if (allFeasibleSize == 0) {
//                hasFeasibleShareLabel = false;
//                break;
//            }

        }

        if (!hasFeasibleShareLabel) {
            res = c.constBoolVal(true);
            for (Automata aut : ca.automataList) {
                for (int k = 0; k < end_step; k++) {
                    for (int m = 0; m < aut.shared_transList.size(); m++) {
                        Term x = ToTerm.transitionTerm(c, aut.ID, aut.shared_transList.get(m), k);
                        res.AND(x.NOT());
                    }
                }
            }
        }
        return res;
    }

    private boolean checkSharedStep(Transition transition, int k) {
        String label = transition.label;
        String[] tmp = label.split("@");
        if (tmp.length > 1) {
            int step = Integer.parseInt(tmp[1]);
            return step <= k;
        }
        return true;
    }

    public Term encodeTrShare() {
        Term res = new Term(true);
        for (Automata aut : ca.automataList) {
            for (int k = 0; k < bound - 1; k++) {

            }
        }

        return res;
    }

    public Term encodeTrGraph() {
        Term res = c.constBoolVal(true);
        for (Automata aut : ca.automataList) {
            ArrayList<Transition> trList = aut.transitions;
            for (int k = 0; k < bound - 1; k++) {
                Term tmp = new Term(false);
                for (int m = 0; m < trList.size(); m++) {
                    Term x = ToTerm.transitionTerm(c, aut.ID, trList.get(m), k);
                    tmp.OR(x);
                    for (int n = m + 1; n < trList.size(); n++) {
                        Term y = ToTerm.transitionTerm(c, aut.ID, trList.get(n), k);
                        res.AND(c.makeOr(x.NOT(), y.NOT()));
                    }
                }
                res.AND(tmp);
//            if(k<=bound-2){
//                Term
//            }
            }
        }
        return res;

    }

//    public Term encodeTrNext(){
//
//    }


    public Term encodeGraph() {
        Term res = c.constBoolVal(true);
        for (Automata aut : ca.automataList) {
            for (int k = 0; k < 1; k++) {
                //对于每一步骤只有一个state被选中
                for (int m = 0; m < aut.locations.size(); m++) {
                    for (int n = m + 1; n < aut.locations.size(); n++) {
                        Term x = ToTerm.stateTerm(c, aut.ID, aut.locations.get(m), k);
                        Term y = ToTerm.stateTerm(c, aut.ID, aut.locations.get(n), k);
                        res.AND(c.makeOr(x.NOT(), y.NOT()));
                    }
                }

//                if(k!=bound){
//                    for (int m=0;m<aut.transitions.size();m++){
//                        Transition tr=aut.transitions.get(m);
//                        Term x=ToTerm.transitionTerm(c,aut.ID,tr,k);
//                        for(int n=m+1;n<aut.transitions.size();n++){
//                            Term y=ToTerm.transitionTerm(c,aut.ID,aut.transitions.get(n),k);
//                            res.AND(c.makeOr(x.NOT(),y.NOT()));
//                        }
//                        if(stutter_enable) {
//                            Term stutter=ToTerm.stutterTerm(c,aut.ID,k);
//                            Term t=c.makeOr(x.NOT(),stutter.NOT());
//                            res.AND(t);
//                        }
//                        Term next_st=ToTerm.stateTerm(c,aut.ID,aut.locations.get(tr.target),k+1);
//                        res.AND(c.makeImply(x,next_st));
//                    }
//                }


            }
            for (int k = 0; k < bound; k++) {
                //todo
            }
        }
        return res;
    }

    public void encodeInit() {

    }

    public void encodeStepSemantic() {

    }

    public void setVar() {
        for (Automata aut : ca.automataList) {
            solver.setVar(aut, bound);
        }
    }

    public void addClause(Term term) {
        if (term.v == null) return;
        for (ArrayList<Integer> t : term.v) {
            solver.addClauses(t);
//            System.out.println(t);
//            System.out.print("[");
//            for (Integer i : t) {
//                System.out.print((i > 0 ? "" : "-") + c.ID2var.get(Math.abs(i)) + ",");
//            }
//            System.out.println("]");
        }
    }

    public ArrayList<int[]> solve() {
        return solver.solve();
    }


    public void clearClause() {
        solver.clearClause();
    }


    public void calFeasibleLabel() {
        for(int k=0;k<bound;k++){
            Map<String, Set<Integer>> labelInCom=new HashMap<>();
            for(String label: ca.sharedlabels){
                labelInCom.put(label,new HashSet<>());
            }
            for(int n=0;n<ca.automataList.size();n++){
                Automata aut=ca.automataList.get(n);
                ArrayList<Transition> allSharedTrans = aut.shared_transList;
                ArrayList<Transition> allFeasibleSharedTrans =new ArrayList<>();
                Set<String> labelSet=new HashSet<>();
                for (Transition tr : allSharedTrans) {
                    if (checkSharedStep(tr, k)) {
                        String label=tr.label.split("@")[0];
                        allFeasibleSharedTrans.add(tr);
                        labelSet.add(label);
                        Set tmp=labelInCom.get(label);
                        tmp.add(n);
                    }
                }

                allFeasibleTr.put(Integer.toString(k)+"-"+Integer.toString(n),allFeasibleSharedTrans);
                allFeasibleLabel.put(Integer.toString(k)+"-"+Integer.toString(n),labelSet);
            }

            for(String label: ca.sharedlabels){
                int size=labelInCom.get(label).size();
                if(size!=ca.shareLabelComponent.get(label).size()){
                    Set tmp=allInfeasibleLabel.getOrDefault(Integer.toString(k),new HashSet<>());
                    tmp.add(label);
                    allInfeasibleLabel.put(Integer.toString(k),tmp);
                }
            }

        }
    }
}

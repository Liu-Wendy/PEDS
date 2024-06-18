package MPC;

import MPC.Solver.AcyclicAutomaton;

import java.util.*;

public class ComposedAutomata {
    public ArrayList<Automata> automataList;
    public Map<String, Automata> automataMap;
    public Set<String> sharedlabels;
    public Set<String> allLables;
    public Map<String ,ArrayList<Automata>> label_automata_map;
    public String objFun;
    public boolean staytime4loc;
    public String forbidden;
    public String feasible_fun;
    public boolean addInit;

    public Set<String> stepSharedLabels;//无环图中所有的shared labels
    public HashMap<Integer,Set<Transition>> eachStepSharedTrans; //每一步中，所有自动机中的共享转换
    public HashMap<String, Set<Integer>> shareLabelComponent;

    // --todo-- shared variables

    public ComposedAutomata(){
        automataList=new ArrayList<>();
        automataMap=new HashMap<>();
        sharedlabels=new HashSet<>();
        allLables= new HashSet<>();
        label_automata_map=new HashMap<>();
        staytime4loc=true;
        addInit=false;
        eachStepSharedTrans=new HashMap<>();
        shareLabelComponent=new HashMap<>();
    }

    public void setAutomaton(Automata aut){
        aut.ID=automataList.size();
        automataMap.put(aut.name,aut);
        automataList.add(aut);
    }

    public void acyclic(int bound){
        ArrayList<AcyclicAutomaton> acyclicAutomata=new ArrayList<>();

        for(Automata aut:automataList){
            AcyclicAutomaton tmp=new AcyclicAutomaton(aut,bound-1);
            acyclicAutomata.add(tmp);
        }

        automataList.clear();

        for(AcyclicAutomaton aut:acyclicAutomata){
            automataList.add(aut.toAutomaton());
        }

        stepSharedLabels=new HashSet<>();
        for(Automata aut:automataList){
            stepSharedLabels.addAll(aut.shared_labels);
            for(Transition tr:aut.shared_transList){
                String t[]=tr.label.split("@");
                int step=Integer.parseInt(t[1]);
                Set<Transition> curStepST=eachStepSharedTrans.getOrDefault(step,new HashSet<>());
                curStepST.add(tr);
                eachStepSharedTrans.put(step,curStepST);
                Set<Integer>set=shareLabelComponent.getOrDefault(t[0],new HashSet<>());
                set.add(aut.ID);
                shareLabelComponent.put(t[0],set);
            }
        }

    }

    public Map<Integer,ArrayList<Transition>> find(int Id, String label, int step) {
        Map<Integer,ArrayList<Transition>> ans=new HashMap<>();
        String[] origin=label.split("@");
        for(Automata aut:automataList){
            if(aut.ID==Id) continue;
            ArrayList<Transition> translist=new ArrayList<>();
            for(Transition tr:aut.transitions){
                String[] tmp=tr.label.split("@");
                if(tmp[0].trim().equals(origin[0].trim())){
                    int nextStep=Integer.parseInt(tmp[1].trim());
                    int curStep=Integer.parseInt(origin[1].trim());
                    if(step==0&&curStep==0){
                        translist.add(tr);
                    }else if(nextStep<=curStep){
                        translist.add(tr);
                    }

                }
//                if(tmp[0].trim().equals(origin[0].trim())){
//                    translist.add(tr);
//                }
//                if(tr.label.equals(label)){
//                    translist.add(tr);
//                }
            }
            if(translist.size()!=0)
                ans.put(aut.ID,translist);
        }
        return ans;
    }

    public Map<Integer,ArrayList<Transition>> find1(int Id, String label,int cur_st) {
        Map<Integer,ArrayList<Transition>> ans=new HashMap<>();
        String[] origin=label.split("@");
        for(Automata aut:automataList){
            if(aut.ID==Id) continue;
            ArrayList<Transition> translist=new ArrayList<>();
            for(Transition tr:aut.transitions){
                String[] tmp=tr.label.split("@");
                if(tmp[0].trim().equals(origin[0].trim())){
//                    translist.add(tr);
                    int step=Integer.parseInt(tmp[1].trim());
                    int curStep=Integer.parseInt(origin[1].trim());
                    if(step<=cur_st){
                        translist.add(tr);
                    }
//                    if(step<=curStep){
//                        translist.add(tr);
//                    }

                }

            }
            if(translist.size()!=0)
                ans.put(aut.ID,translist);
        }
        return ans;
    }

    public Map<Integer,ArrayList<Transition>> findByStep(int Id, String label,int step) {
        Map<Integer,ArrayList<Transition>> ans=new HashMap<>();
        for(int k=0;k<=step;k++){
            Set<Transition> trs=eachStepSharedTrans.getOrDefault(k,new HashSet<>());
            if(trs.isEmpty()) continue;

            for(Transition tr:trs){
                if(tr.autId!=Id){
                    ArrayList<Transition> tmp=ans.getOrDefault(tr.autId,new ArrayList<>());
                    tmp.add(tr);
                    ans.put(tr.autId,tmp);
                }
            }
        }
        return ans;
    }
}

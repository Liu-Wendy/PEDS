package MPC;

import MPC.Solver.Encoder;
import MPC.Solver.ListOperationContext;
import MPC.Solver.SATSolver;
import MPC.Solver.Term;
import Racos.Componet.Instance;
import Racos.Method.Continue;
import Racos.ObjectiveFunction.ObjectFunction;
import Racos.ObjectiveFunction.Task;
import Racos.Time.TimeAnalyst;
import Racos.Tools.ValueArc;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;

public class Combination {
    public Parser parser;
    public ComposedAutomata ca;
    public Automata product;
    public TimeAnalyst timeAnalyst;
    public String forbiddenConstraints;
    public ArrayList<Automata> automata;
    public int automata_num;
    File output;
    BufferedWriter bufferedWriter;
    public ArrayList<ArrayList<Integer>> PathMap;
    //    public ArrayList<ArrayList<Integer>> combin;
    public String forbidden;
    public double delta;
    public double cycle;
    public ValueArc minValueArc;
    public ListOperationContext c;
    public Encoder encoder;
    public Map<Integer, Map<Integer, ArrayList<ArrayList<Integer>>>> allPaths;
    public long startMemory;
    public Runtime runtime;
    public int feasibla_target;
    public int bound;
    public Map<Integer,Map> shareInfo;

    public Combination(String modelFile, String cfgFile, int bound) {
        try {
            runtime = Runtime.getRuntime();
            runtime.gc();
            timeAnalyst = new TimeAnalyst();
            double before = System.currentTimeMillis();
            parser = new Parser(modelFile);
            ca = parser.generateComposedAutomaton(cfgFile, bound);
            double after = System.currentTimeMillis();

            timeAnalyst.addParseTime((after - before) / 1000);

            automata_num = ca.automataList.size();
            delta = ca.automataList.get(0).delta;
            c = new ListOperationContext();
            before = System.currentTimeMillis();
//            findPath(c, bound);
            this.bound=bound;
            shareInfo=new HashMap<>();
            findPathOtimal(c, bound);
            after = System.currentTimeMillis();
            timeAnalyst.addEncodeTime((after - before) / 1000);
            System.out.println((after - before) / 1000);

        } catch (IOException e) {
            System.out.println("Open file fail!");
        } catch (ParserConfigurationException | SAXException ignored) {
        }
    }

    private ArrayList<ArrayList<Integer>> decodePath(int autID, ArrayList<int[]> solutions) {
        int initLoc = ca.automataList.get(autID).initLoc;
        ArrayList<Transition> trans = ca.automataList.get(autID).transitions;
        ArrayList<ArrayList<Integer>> rtn = new ArrayList<>();
        for (int[] solution : solutions) {

            ArrayList<Integer> path1 = new ArrayList<>(bound);
            for(int m=0;m<bound;m++){
                path1.add(-1);
            }

            path1.set(0,initLoc);
            int index = 1;
            for (int j : solution) {
                if (j < 0) continue;
                String var = c.getVar(j);
                String[] parts = var.split("_");
                int step=Integer.parseInt(parts[3]);
                if (parts[4].contains("stutter")) {
                    String[] tmp = parts[4].split("#");

                    path1.set(step+1,Integer.parseInt(tmp[2]));
                } else {
                    int nextLoc = trans.get(Integer.parseInt(parts[2])).target;
                    path1.set(step+1,nextLoc);

                }
            }
            //去stutter
            ArrayList<Integer> path2 = new ArrayList<>();
            int last=-1;
            for(int i=0;i<bound;i++){
                int curloc=path1.get(i);
                if(curloc==-1) break;

                if(last==-1){
                    path2.add(curloc);
                    last=curloc;
                }else{
                    if(curloc==last) continue;
                    path2.add(curloc);
                    last=curloc;
                }
            }
            rtn.add(path2);
        }
        return rtn;
    }

    private ArrayList<ArrayList<Integer>> decodePath1(int autID, ArrayList<int[]> solutions) {
        ArrayList<Transition> trans = ca.automataList.get(autID).transitions;
        ArrayList<ArrayList<Integer>> rtn = new ArrayList<>();
        for (int[] solution : solutions) {

            ArrayList<Integer> path1 = new ArrayList<>(bound);
            for(int m=0;m<bound;m++){
                path1.add(-2);
            }

            for (int j : solution) {
                if (j < 0) continue;
                String var = c.getVar(j);
                String[] parts = var.split("_");
                int step=Integer.parseInt(parts[3]);
                if (parts[4].contains("stutter")){
                    path1.set(step,-1);
                }else
                    path1.set(step,Integer.parseInt(parts[2]));
            }

            rtn.add(path1);
        }
        return rtn;
    }

    private void findPath(ListOperationContext c, int bound) {
        encoder = new Encoder(ca, new SATSolver(), c, bound);
        allPaths = new HashMap<>();
        int index = -1;
        ArrayList<Integer> ansSize = new ArrayList<>();

        int size = 0;

        //给trans编码，找到所有可行的label位置
        for (int i = 0; i < bound; i++) {
            encoder.clearClause();
            Term x = encoder.encodeTrInit(i - 1);
            encoder.addClause(x);

            Term y = encoder.encodeTrGraph(i - 1);
            encoder.addClause(y);


            System.out.println("step: " + i);
            ArrayList<int[]> ans = encoder.solve();
            c.show(ans);
            size += ans.size();
        }

        System.out.println(size);
    }


    private void findPathOtimal(ListOperationContext c, int bound) {
        Map<String, Term> termMap = new HashMap<>();
        encoder = new Encoder(ca, new SATSolver(), c, bound);
        allPaths = new HashMap<>();
        int index = -1;
        encoder.calFeasibleLabel();

        ArrayList<Integer> ansSize = new ArrayList<>();
        System.out.println("Current used memory:" + (runtime.totalMemory() - runtime.freeMemory() - startMemory) / 1024 / 1024);
        //给trans编码，找到所有可行的label位置
        for (int i = 0; i < bound; i++) {
            if (i == 0 && ca.addInit) continue;

            encoder.clearClause();
            for (Automata aut : ca.automataList) {
                //第一个转化只能是初始节点的后续转换和stutter
                //注意这里自动机中已经给aut整体trans以及每个节点的next_trans加了stutter
                Term inTr = encoder.encodeTrInitForSingleAut(aut.ID, i - 1);
                termMap.put("init" + Integer.toString(i) + Integer.toString(aut.ID), inTr);
//                encoder.addClause(inTr);
                //每一次转化只有一条transition被选中
                Term graph_t = encoder.encodeTrGraphForSingleAut(aut.ID, i - 1);
                termMap.put("graph" + Integer.toString(i) + Integer.toString(aut.ID), graph_t);
            }

//            Term init=encoder.encodeTrInit(i-1);
//            encoder.addClause(init);

            Term shareTerm = encoder.encodeLabel(i);
            encoder.addClause(shareTerm);


            System.out.println("step: " + i);
//            System.out.println("var num: "+c.ID2var.size());
//            System.out.println("All vars:");
//            for(Map.Entry<Integer,String> entry:c.ID2var.entrySet()){
//                System.out.println("["+entry.getKey()+", "+entry.getValue()+"]");
//            }
            ArrayList<int[]> ans =  encoder.solve();
//            System.out.println("Shared label sequence:");
//            c.show(ans);
            ansSize.add(ans.size());

//            System.out.println("shared-label sequence size:"+ans.size());
//            if(i==4) {
//                int[] solution11 = ans.get(27394);
//                for (int j : solution11) {
//                    if (j < 0) continue;
//                    System.out.print(c.ID2var.get(j) + " ");
//                }
//                System.out.println(" ");
//            }
            int as=0;
            //为每个解，求单独的PATH
            for (int[] solution : ans) {
//                as++;
//                if(as==27394){
//                    System.out.println("here");
//                    for (int j : solution) {
//                        if (j < 0) continue;
//                        System.out.print(c.ID2var.get(j) + " ");
//                    }
//                    System.out.println(" ");
//                }
                Map<Integer, ArrayList<ArrayList<Integer>>> eachSolutionPaths = new HashMap<>();
                Map<Integer, Term> decodeTerm = decodeTerm1(c,solution,index);
                if(decodeTerm==null) continue;
                for (Automata aut : ca.automataList) {
                    encoder.clearClause();
                    encoder.addClause(termMap.get("init" + Integer.toString(i) + Integer.toString(aut.ID)));
                    encoder.addClause(termMap.get("graph" + Integer.toString(i) + Integer.toString(aut.ID)));

                    encoder.addClause(decodeTerm.getOrDefault(aut.ID, new Term(true)));

//                    System.out.println("All vars:");
//                    for (Map.Entry<Integer, String> entry : c.ID2var.entrySet()) {
//                        System.out.println("[" + entry.getKey() + ", " + entry.getValue() + "]");
//                    }
                    ArrayList<int[]> tmp = encoder.solve();

                    if (tmp.size() == 0) break;
//                    System.out.println("component path:");
//                    if(as==27394) {
//                        c.show(tmp);
//                    }
//                    System.out.println("【AUT】 " + aut.name);
//                    System.out.println("step: " + i);
//                    System.out.println("var num: " + c.ID2var.size());
                      eachSolutionPaths.put(aut.ID, decodePath1(aut.ID, tmp));

                }
                if (eachSolutionPaths.size() == ca.automataList.size()) {
                    allPaths.put(++index, eachSolutionPaths);
                }
            }

        }
        for (int t = 0; t < ansSize.size(); t++) {
            System.out.println("step " + (t + 1) + ":" + ansSize.get(t));
        }
        System.out.println("all solutions:" + allPaths.size());
        int size = 0;
        for (int i = 0; i < allPaths.size(); i++) {
            int t = 1;
            for (int n = 0; n < automata_num; n++) {
                t *= allPaths.get(i).get(n).size();
            }
            size += t;
        }
        System.out.println("all global paths:" + size);

    }

    public Map<Integer, Term> decodeTerm1(ListOperationContext c, int[] solution, int index) {
        Map<String, Map<Integer,ArrayList<Integer>>> eachStep = new HashMap<>();

        Map<Integer, Term> rtn = new HashMap<>();
        for (int j : solution) {

            int id = Math.abs(j);
            String var = c.ID2var.get(id);

            String[] parts = var.split("_");
            if (parts.length > 2) {
                Term tmp = rtn.getOrDefault(Integer.parseInt(parts[1]), c.constBoolVal(true));
                tmp.AND(new Term(j));
                rtn.put(Integer.parseInt(parts[1]), tmp);
            }

            if (j > 0 && parts.length>=5) {
                String label=parts[4].split("@")[0];
                if(ca.sharedlabels.contains(label)){
                    Map<Integer,ArrayList<Integer>> map=eachStep.getOrDefault(label,new HashMap<>());

                    ArrayList<Integer> list=map.getOrDefault(Integer.parseInt(parts[1]),new ArrayList<>());
                    list.add(Integer.parseInt(parts[3]));
                    map.put(Integer.parseInt(parts[1]),list);

                    eachStep.put(label,map);
                }
            }
        }

        for(Map.Entry<String, Map<Integer,ArrayList<Integer>>>entry: eachStep.entrySet()){
            Map<Integer,ArrayList<Integer>> map=entry.getValue();
            if(map.size()==1) return null;
            if(map.size()!=0){
                int last=-1;
                for(Map.Entry<Integer,ArrayList<Integer>> cnt: map.entrySet()){
                    int num=cnt.getValue().size();
                    if(last!=-1&&num!=last) return null;
                    if(last==-1&&num!=0) last=num;
                }
            }
        }
        shareInfo.put(index+1,eachStep);
        return rtn;
//
//        // 将HashMap中的entry集合转换为List
//        List<Map.Entry<String, Map<Integer,ArrayList<Integer>>>> list = new ArrayList<>(eachStep.entrySet());
//
//        // 使用Collections.sort()方法，根据entry的值进行排序
//        Collections.sort(list, new Comparator<Map.Entry<String, Map<Integer,ArrayList<Integer>>>>() {
//            @Override
//            public int compare(Map.Entry<String, Map<Integer,ArrayList<Integer>>> o1, Map.Entry<String, Map<Integer,ArrayList<Integer>>> o2) {
//
//
//
//                return o1.getValue().compareTo(o2.getValue());
//            }
//        });
//
//        // 创建一个新的有序HashMap来存储排序后的结果
//        LinkedHashMap<String, Integer> sortedMap = new LinkedHashMap<>();
//        for (Map.Entry<String, Integer> entry : list) {
//            sortedMap.put(entry.getKey(), entry.getValue());
//        }
//


    }

    public void print(String str) {
        try {
            bufferedWriter.write(str);
        } catch (IOException e) {
            System.out.println("write to file error!");
        }
    }

    public void println(String str) {
        try {
            bufferedWriter.write(str + "\n");
        } catch (IOException e) {
            System.out.println("write to file error!");
        }
    }

    double[] runRacos(Combination combination, int maxPathsize) {
        int samplesize = 20;       // parameter: the number of samples in each iteration
        int iteration = 1000;       // parameter: the number of iterations for batch racos
        int budget = 2000;         // parameter: the budget of sampling for sequential racos
        int positivenum = 2;       // parameter: the number of positive instances in each iteration
        double probability = 0.75; // parameter: the probability of sampling from the model
        int uncertainbit = 4;      // parameter: the number of sampled dimensions
        Instance ins = null;
        int repeat = 1;
        Task t = new ObjectFunction(combination, maxPathsize, timeAnalyst);
        double[] info = new double[3];
        for (int i = 0; i < repeat; i++) {
            double currentT = System.currentTimeMillis();
            Continue con = new Continue(t, combination);
            con.setMaxIteration(iteration);
            con.setSampleSize(samplesize);      // parameter: the number of samples in each iteration
            con.setBudget(budget);              // parameter: the budget of sampling
            con.setPositiveNum(positivenum);    // parameter: the number of positive instances in each iteration
            con.setRandProbability(probability);// parameter: the probability of sampling from the model
            con.setUncertainBits(uncertainbit); // parameter: the number of samplable dimensions
            con.setBound(maxPathsize);
            con.setPathMap(allPaths);
            ValueArc valueArc = con.run();                          // call sequential Racos              // call Racos


            double currentT2 = System.currentTimeMillis();
            ins = con.getOptimal();             // obtain optimal

            System.out.print("best function value:");
            System.out.println(ins.getValue() + "     ");
            info[0] = ins.getValue();


            System.out.print("[");

            for (int j = 0; j < ins.getFeature().length; ++j) {
                System.out.print(Double.toString(ins.getFeature(j)) + ",");
            }
            System.out.println("]");

            info[1]=valueArc.iterativeNums;
        }
        return info;
    }


    public static void main(String[] args) {
        configUtil config = new configUtil();
        String prefix = config.get("system") + "_" + config.get("number");
        String modelFile = prefix + ".xml";
        String cfgFile = prefix + ".cfg";

        try {
            int bound = Integer.parseInt(config.get("bound"));
            File result = new File("results/new_c_" + prefix + "_bound_" + bound + ".txt");
            BufferedWriter buffer = new BufferedWriter(new FileWriter(result));

            int round = Integer.parseInt(config.get("round"));

            while (round > 0) {

                double currentTime = System.currentTimeMillis();

                Combination combination = new Combination(modelFile, cfgFile, bound);

                Runtime r = combination.runtime;
                r.gc();
                long startMem = r.totalMemory() - r.freeMemory();
                combination.startMemory = startMem;
                combination.feasibla_target=Integer.parseInt(config.get("feasible_target"));

                double[] ans = combination.runRacos(combination, bound);
                int s = combination.timeAnalyst.getCnt();
                double endTime = System.currentTimeMillis();
                double t = (endTime - currentTime) / 1000;
//                double endTime = System.currentTimeMillis();
//                double t = (endTime - currentTime) / 1000;
                System.out.println("time cost: " + t);
                System.out.println("ODE time: " + combination.timeAnalyst.getODETime());
                System.out.println("forbidden time: " + combination.timeAnalyst.getForbiddenTime());
                System.out.println("encode and find path time: " + combination.timeAnalyst.getEncodeTime());

                System.out.println("sample size: " + s);
                System.out.println("average ODE time: " + combination.timeAnalyst.getODETime() / s);
                System.out.println("average flow time: " + combination.timeAnalyst.getFlowTime() / s);
                System.out.println("average forbidden time: " + combination.timeAnalyst.getForbiddenTime() / s);

                long endMen = r.totalMemory() - r.freeMemory();
//                String tmp = Double.toString(t) + " " + Double.toString(ans[0])
////                        +" "+Double.toString(combination.timeAnalyst)
//                        + "\n";

                String tmp = Double.toString(ans[0])
                        + " " + Double.toString(t) + " " + Double.toString(combination.timeAnalyst.encodeTime) + " "
                        + Double.toString(combination.timeAnalyst.getODETime()) + " "
                        + Integer.toString(combination.timeAnalyst.getCnt())
                        + " " + Long.toString((endMen - startMem) / 1024 / 1024)
                        + " " +Double.toString(ans[1])
                        + "\n";

                try {
                    buffer.write(tmp);
                } catch (IOException e) {
                    System.out.println("write to file error!");
                }
                round--;
            }
            buffer.close();
        } catch (IOException e) {
            System.out.println("Open result.txt fail!");
        }

    }
}




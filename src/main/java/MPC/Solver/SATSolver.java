package MPC.Solver;

import MPC.Automata;
import MPC.Location;
import MPC.Transition;
import org.sat4j.core.Vec;
import org.sat4j.core.VecInt;
import org.sat4j.minisat.SolverFactory;
import org.sat4j.specs.*;
import org.sat4j.tools.ModelIterator;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;


public class SATSolver {
    public ArrayList<String> varList;
    public Map<String, Integer> var2ID;
    public Map<Integer, String> ID2var;
    public int nr_vars;
    public ArrayList<ArrayList<Integer>> clauses;

    public ISolver solver;


    public SATSolver() {
        var2ID = new HashMap<>();
        ID2var = new HashMap<>();
        nr_vars = 0;
        clauses=new ArrayList<>();
    }

    public void addClauses(ArrayList<Integer> clause){
        clauses.add(clause);
    }


    public ArrayList<int[]> solve() {

        solver = SolverFactory.newDefault();
        try {

//            System.out.println("All clauses:");
            for (ArrayList<Integer> clause : clauses) {
//                System.out.println(clause);
                int len = clause.size();
                int[] tmp = new int[len];
                for (int j = 0; j < len; j++) {
                    tmp[j] = clause.get(j);
                }
                VecInt vecInt = new VecInt(tmp);
                solver.addClause(vecInt);

            }

            ModelIterator iterator = new ModelIterator(solver);
            ArrayList<int[]> ans=new ArrayList<>();
            if (iterator.isSatisfiable()) {
                while (iterator.isSatisfiable()) {
                    int[] model = iterator.model();
                    ans.add(model);
//                    System.out.print("Solution: ");
//                    for (int i = 0; i < model.length; i++) {
//                        if (model[i] > 0) {
//                            System.out.print(model[i] + "  ");
//                        }
//                    }
//                    System.out.println(" ");
//                System.out.println("Solution: " + java.util.Arrays.toString(model));
                }
            }
            return ans;

        } catch (ContradictionException e) {
            System.out.println("Unsatisfiable (trivial)!");
        } catch (TimeoutException e) {
            System.out.println("Timeout, sorry!");
        }
        return new ArrayList<>();
    }

    public static void main(String[] args) {

        SATSolver s = new SATSolver();
        s.solve();
    }


    public void setVar(Automata aut) {
        for (Map.Entry<Integer, Location> entry : aut.locations.entrySet()) {
            String tmp = "st_" + aut.ID + "_" + entry.getValue().name;
            varList.add(tmp);
        }

        for (Transition tr : aut.transitions) {

        }
    }



    public void setVar(Automata aut, int bound) {
        for (int k = 0; k < bound; k++) {
            for (Map.Entry<Integer, Location> entry : aut.locations.entrySet()) {
                String tmp = "st_" + aut.ID + "_" + k + "_" + entry.getValue().name;
                assertVar(tmp);
            }

            for (Transition tr : aut.transitions) {
                String tmp = "st_" + aut.ID + "_" + tr.id + "_" + k + "_" + tr.label;
                assertVar(tmp);
            }
            String tmp = "st_" + aut.ID + "_-1_" + k + "_stutter";
            assertVar(tmp);

            tmp = "end_" + aut.ID + "_" + k;
            assertVar(tmp);
        }
    }

    private void assertVar(String tmp) {
        if (!var2ID.containsKey(tmp)) {
            nr_vars++;
            var2ID.put(tmp, nr_vars);
            ID2var.put(nr_vars,tmp);
            varList.add(tmp);
        }
    }


    public void clearClause() {
        clauses.clear();
    }
}


package MPC.Solver;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ListOperationContext {

    public Map<String, Integer> var2ID;
    public Map<Integer, String> ID2var;
    public int nr_vars;

    public ListOperationContext() {
        var2ID = new HashMap<>();
        ID2var = new HashMap<>();
        nr_vars = 0;
    }

    public Term constBoolVal(boolean val) {
        return new Term(val);
    }

    public Term getBoolTerm(String name) {
        if (!var2ID.containsKey(name)) {
            nr_vars++;
            var2ID.put(name, nr_vars);
            ID2var.put(nr_vars, name);
            return new Term(nr_vars);
        } else {
            return new Term(var2ID.get(name));
        }
    }

    public Term makeAnd(Term a, Term b) {
        Term rtn = new Term();
        rtn.v.addAll(a.v);
        rtn.v.addAll(b.v);

        return rtn;
    }

    public Term makeOr(Term a, Term b) {
        Term rtn = new Term();
        for (ArrayList<Integer> c1 : a.v) {
            for (ArrayList<Integer> c2 : b.v) {
                ArrayList<Integer> tmp = new ArrayList<>();
                tmp.addAll(c1);
                tmp.addAll(c2);
                rtn.v.add(tmp);
            }
        }
        return rtn;
    }

    public Term makeNot(Term a) {
        Term rtn = new Term(false);
        for (ArrayList<Integer> c : a.v) {
            Term tmp = new Term();
            ArrayList<Integer> t = new ArrayList<>();
            for (int i : c) {
                t.add(-i);
            }
            tmp.v.add(t);
            rtn = makeOr(rtn, tmp);
        }
        return rtn;
    }

    public Term makeImply(Term a, Term b) {
        return makeOr(b, makeNot(a));
    }


    public void show(ArrayList<int[]> ans) {
        int index=0;
        for (int[] a : ans) {
            System.out.print("Path:"+(++index));

            for (int j : a) {
                if (j < 0) continue;
                System.out.print(ID2var.get(j) + " ");
            }
            System.out.println(" ");
        }
    }

    public Map<Integer, Term> decodeTerm(int[] solution) {
        Map<String, ArrayList<Integer>> eachCnt = new HashMap<>();

        Map<Integer, Term> rtn = new HashMap<>();
        for (int j : solution) {

            int id = Math.abs(j);
            String var = ID2var.get(id);

            String[] parts = var.split("_");
            if (parts.length > 2) {
                Term tmp = rtn.getOrDefault(Integer.parseInt(parts[1]), constBoolVal(true));
                tmp.AND(new Term(j));
                rtn.put(Integer.parseInt(parts[1]), tmp);
            }

        }

        return rtn;
    }

    public String getVar(int j) {
        int id = Math.abs(j);
        return ID2var.get(id);
    }
}

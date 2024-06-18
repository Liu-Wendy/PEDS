package MPC.Solver;

import java.util.ArrayList;
import java.util.Collections;

public class Term {
    public ArrayList<ArrayList<Integer>> v;

    public Term(){
        if(v==null)
            v=new ArrayList<>();
    }
    public Term(int[] a){
        this();
        for (int j : a) {
            ArrayList<Integer> tmp = new ArrayList<>();
            tmp.add(j);
            v.add(tmp);
        }
    }
    public Term(int var){
        this();
        v.add(new ArrayList<Integer>(Collections.nCopies(1,var)));
    }

    public Term(boolean val){
        this();
        if(!val){
            ArrayList<Integer> tmp=new ArrayList<>();
            v.add(tmp);
        }
    }

    public void OR(Term b){
        if(v.size()==0){
            return ;
        }
        if(b.v.size()==0){
            v.clear();
            return ;
        }

        for (int i=0;i<b.v.get(0).size();i++){
            int l=b.v.get(0).get(i);
            v.get(0).add(l);
        }

    }

    public void AND(Term b){
        for(int i=0;i<b.v.size();i++){
            ArrayList<Integer> clause=b.v.get(i);
            v.add(clause);
        }

    }

    public Term NOT(){

        if(v.size()>1){
            return this;
        }
        Term y=new Term();
        int t=v.get(0).get(0);
        ArrayList<Integer> tmp=new ArrayList<>();
        tmp.add(-t);
        y.v.add(tmp);
        return y;
    }
    //\= &= makeOr



}

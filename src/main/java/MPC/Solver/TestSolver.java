package MPC.Solver;

import org.sat4j.core.VecInt;
import org.sat4j.minisat.SolverFactory;
import org.sat4j.specs.ContradictionException;
import org.sat4j.specs.ISolver;
import org.sat4j.specs.TimeoutException;
import org.sat4j.tools.ModelIterator;

public class TestSolver {
    public static void main(String[] args) throws ContradictionException, TimeoutException {
        ISolver solver = SolverFactory.newDefault();

        int[][] clauses = {
                {-1,-5},
                {-1,-4}
        };

        for (int[] clause : clauses) {
            VecInt vecInt = new VecInt(clause);
            solver.addClause(vecInt);
        }

        ModelIterator iterator = new ModelIterator(solver);

        while (iterator.isSatisfiable()) {
            int[] model = iterator.model();
            System.out.println("Solution: " + java.util.Arrays.toString(model));
        }
    }
}
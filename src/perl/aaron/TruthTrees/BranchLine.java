package perl.aaron.TruthTrees;

import java.awt.Color;
import java.awt.FontMetrics;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import perl.aaron.TruthTrees.logic.AtomicStatement;
import perl.aaron.TruthTrees.logic.Composable;
import perl.aaron.TruthTrees.logic.Decomposable;
import perl.aaron.TruthTrees.logic.Statement;
import perl.aaron.TruthTrees.logic.Negation;

/**
 * A class that represents a single line in a branch, used for storing and verifying decompositions
 * @author Aaron
 *
 */
public class BranchLine {
	protected Branch parent;
	protected Statement statement;
//	protected Set<Set<BranchLine>> decomposition;
	protected Set<Branch> selectedBranches; // holds the parent of the split that decomposes this line
	protected Set<BranchLine> selectedLines;
	protected BranchLine decomposedFrom;
	protected boolean isPremise;
	public static final Color SELECTED_COLOR = new Color(0.3f,0.9f,0.9f);
	public static final Color DEFAULT_COLOR = Color.LIGHT_GRAY;
	public static final Color EDIT_COLOR = Color.GREEN;
  public boolean typing = false;
  public String currentTyping;

	public BranchLine(Branch branch)
	{
		parent = branch;
		statement = null;
//		decomposition = new LinkedHashSet<Set<BranchLine>>();
		selectedBranches = new LinkedHashSet<Branch>();
		selectedLines = new LinkedHashSet<BranchLine>();
		isPremise = false;
	}

	public String toString()
	{
		if (statement != null)
			return statement.toString();
		return "";
	}
	
	public void setIsPremise(boolean isPremise)
	{
		this.isPremise = isPremise;
	}
	
	public boolean isPremise()
	{
		return isPremise;
	}
	
	public void setStatement(Statement statement)
	{
		this.statement = statement;
	}
	
	public Statement getStatement()
	{
		return statement;
	}
	
	public int getWidth(FontMetrics f)
	{
    if (typing)
      return f.stringWidth(currentTyping);
    else
      return f.stringWidth(toString());
	}
	
	public Set<BranchLine> getSelectedLines()
	{
		return selectedLines;
	}
	
	public Set<Branch> getSelectedBranches()
	{
		return selectedBranches;
	}
	
	public void setDecomposedFrom(BranchLine decomposedFrom)
	{
		this.decomposedFrom = decomposedFrom;
	}
	
	public BranchLine getDecomposedFrom()
	{
		return decomposedFrom;
	}
	
	public Branch getParent()
	{
		return parent;
	}
	
	
	public String verifyDecomposition()
	{
		// Check if the statement is decomposable and it is not the negation of an atomic statement
		if (statement == null)
			return null;
		
		// also check if the statement is not branching on any statement + negation of that statement
		if (verifyIsBranchOn()) {
			return null;
		}
		
		if (statement instanceof Composable) {
			String resultComposable = ((Composable) statement).verifyComposition(selectedLines);
			
			if (resultComposable.equals("composable")) {
				return null;
			}
			if (!resultComposable.equals("X")) {
				return resultComposable;
			}
		}
		
		if (decomposedFrom == null && !isPremise) {
			return "Unexpected statement \"" + statement.toString() + "\" in tree";

		}
		if (statement instanceof Decomposable &&
				!(statement instanceof Negation && (((Negation)statement).getNegand() instanceof AtomicStatement)))
		{
			if (selectedBranches.size() > 0) // branching decomposition (disjunction)
			{
				Set<BranchLine> usedLines = new LinkedHashSet<BranchLine>();
				for (Branch curRootBranch : selectedBranches)
				{
					List<List<Statement>> curTotalSet = new ArrayList<List<Statement>>();
					for (Branch curBranch : curRootBranch.getBranches())
					{
						List<Statement> curBranchSet = new ArrayList<Statement>();
						for (BranchLine curLine : selectedLines)
						{
							if (curLine.getParent() == curBranch)
							{
								curBranchSet.add(curLine.getStatement());
								usedLines.add(curLine);
							}
						}
						curTotalSet.add(curBranchSet);
					}
					if (selectedLines.size() > 0 &&
							!((Decomposable)statement).verifyDecomposition(curTotalSet,
							parent.getConstants(),
							parent.getConstantsBefore(selectedLines.iterator().next())))
						return "Invalid decomposition of statement \"" + statement.toString() + "\"";
				}
				if (!usedLines.equals(selectedLines)) // extra lines that were unused
					return "Too many statements decomposed from \"" + statement.toString() + "\"";
				if (!BranchLine.satisfiesAllBranches(parent, selectedBranches))
					return "Statement \"" + statement.toString() + "\" not decomposed in every child branch";
			}
			else // non-branching decomposition (conjunction)
			{
				// A map of leaf branches to a list of statements in that branch and up that are selected
				Map<Branch, List<Statement>> branchMap = new LinkedHashMap<Branch, List<Statement>>();
				Set<Branch> selectedBranches = new LinkedHashSet<Branch>();
				// Add all branches that contain selected lines
				for (BranchLine curLine : selectedLines)
				{
					selectedBranches.add(curLine.getParent());
				}
				for (BranchLine curLine : selectedLines)
				{
					List<Statement> curList = null;
					// Check if this branch is in the map and add the statement to it
					if (branchMap.containsKey(curLine.getParent()))
						curList = branchMap.get(curLine.getParent());
					else // Check for child branches and add this line to all of those
					{
						boolean foundChildren = false;
						for (Branch curBranch : selectedBranches)
						{
							if (curBranch != curLine.getParent() && curBranch.isChildOf(curLine.getParent()))
							{
								System.out.println("Found child of " + curLine.getStatement());
								foundChildren = true;
								if (branchMap.containsKey(curBranch))
								{
									branchMap.get(curBranch).add(curLine.getStatement());
								}
								else
								{
									List<Statement> newList = new ArrayList<Statement>();
									newList.add(curLine.getStatement());
									branchMap.put(curBranch, newList);
								}
							}
						}
						if (!foundChildren)
						{
							curList = new ArrayList<Statement>();
							branchMap.put(curLine.getParent(), curList);
						}
					}
					if (curList != null)
						curList.add(curLine.getStatement());
				}
				for (Branch curBranch : branchMap.keySet())
				{
					List<List<Statement>> currentDecomp = new ArrayList<List<Statement>>();
					currentDecomp.add(branchMap.get(curBranch));
					if (!((Decomposable) statement).verifyDecomposition(currentDecomp,
							curBranch.getConstants(),
							curBranch.getConstantsBefore(selectedLines.iterator().next())))
					{
						return "Invalid decomposition of statement \"" + statement.toString() + "\"";
					}
				}
				if (branchMap.size() == 0)
				{
					List<List<Statement>> currentDecomp = Collections.emptyList();
					Set<String> constants = Collections.emptySet();
					if (!((Decomposable) statement).verifyDecomposition(currentDecomp,constants,constants))
						return "Statement \"" + statement.toString() + "\" has not been decomposed!";
					else
						return null;
				}
				if(!BranchLine.satisfiesAllBranches(parent, branchMap.keySet()))
				{
					return "Statement \"" + statement.toString() + "\" not decomposed in every child branch";
				}
			}
		}
		return null;
	}
	
	/**
	 * Verifies if this statement is part of a statement and its negation branching (branch on any P and ~P, for example)
	 * @return true if this is a valid BranchOn, false otherwise
	 */
	private boolean verifyIsBranchOn() {

		// if the direct root has more or less than 2 branches, or if this BranchLine is not the first BranchLine in this branch,
		// then will return false 
		if (parent.getRoot() == null || parent.getRoot().getBranches().size() != 2 || !parent.getStatement(0).equals(statement)) {
			return false;
		}
		else { // compare to the first BranchLine in the sister branch to see if they are each other's negations
			Iterator<Branch> branchItr = parent.getRoot().getBranches().iterator();

			while (branchItr.hasNext()) {
				Branch temp = branchItr.next();
				if (!temp.getStatement(0).equals(statement)) { // then it is the other branch in this set of two branches
					if ((temp.getStatement(0) instanceof Negation && ((Negation)temp.getStatement(0)).getNegand().equals(statement)) ||
							(statement instanceof Negation && ((Negation)statement).getNegand().equals(temp.getStatement(0))) ) {
						return true;
					} else {
						return false;
					}
				}
			}
			return false;
		}
		
		
		
	}

	public static boolean satisfiesAllBranches(Branch root, Set<Branch> descendents)
	{
		if (descendents.contains(root) || root.isClosed())
			return true;
		else
		{
			if (root.getBranches().size() > 0)
			{
				for (Branch curBranch : root.getBranches())
				{
					if (!satisfiesAllBranches(curBranch, descendents))
						return false;
				}
				return true;
			}
			else
				return false;
		}
	}
	
}

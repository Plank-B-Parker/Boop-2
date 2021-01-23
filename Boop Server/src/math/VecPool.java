package math;

import java.util.ArrayList;
import java.util.Stack;

public class VecPool{
	
	private ArrayList<Vec2f> vecs = new ArrayList<>();
	private int vP = 0;//Pointer to first inactive vector.
	private Stack<Integer> vecsToClear = new Stack<Integer>();
	
	
	public VecPool() {
		
	}
	
	/**
	 * Call this method before getting any temporary variables from this class.
	 * Ideally at start of the method where temporary variable is used.
	 */
	public void startOfMethod() {
		vecsToClear.push(0);
	}
	
	/**
	 * Call this method before the return of the function where temporary variable is being used. 
	 */
	public void endOfMethod() {
		cleanVecs();
	}
	
	////////////////////////////////////////////

	/**
	 * Returns a temporary vector to be used.
	 * @return a Vec2f object.
	 */
	public Vec2f getVec() {
		Vec2f vec = null;
		
		if(vecs.size() == vP) 
			vecs.add(vec = new Vec2f());
		else 
			vec = vecs.get(vP);
		vP++;
		
		int numVectsToClear = vecsToClear.pop() + 1;
		vecsToClear.push(numVectsToClear);
		
		return vec;
	}
	
	/**
	 * Clear the last num vecs.
	 * @param num
	 */
	private void clearVecs(int num) {
		if(vP > 0)
			vP -= num;
	}
	
	private void cleanVecs() {
		clearVecs(vecsToClear.pop());
	}
	
	//////////////////////////////////////////////
}

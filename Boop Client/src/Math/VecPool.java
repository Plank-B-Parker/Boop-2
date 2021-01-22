package Math;

import java.util.ArrayList;
import java.util.Stack;

public class VecPool{
	
	private ArrayList<Vec2f> vecs = new ArrayList<>();
	private int vP = 0;//Pointer to first inactive vector.
	private Stack<Integer> vecsToClear = new Stack<Integer>();
	
	
	public VecPool() {
		
	}
	
	public void startOfMethod() {
		vecsToClear.push(0);
	}
	
	public void endOfMethod() {
		cleanVecs();
	}
	
	////////////////////////////////////////////

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

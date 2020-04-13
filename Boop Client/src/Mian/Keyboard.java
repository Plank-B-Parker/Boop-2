package Mian;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import Math.Vec2f;

public class Keyboard {
	
	private Map<Key, Boolean> keyMap = new EnumMap<>(Key.class);
	private ArrayList<Method> methods = new ArrayList<>(10);
	private List<Method> methodList;
	
	public Keyboard() throws NoSuchMethodException{
		
		addMethodsToList();
		
		for (Key key: Key.values()) {
			keyMap.put(key, false);
		}
		
		methodList = methods.subList(0, 10);
		methodList = Collections.unmodifiableList(methodList);
	}
	
	// Methods should only be from the Networking class and be sending information
	// to the server.
	private void addMethodsToList() throws NoSuchMethodException{
		Method method = Vec2f.class.getMethod("copy", null);
		methods.add(method);
	}

}

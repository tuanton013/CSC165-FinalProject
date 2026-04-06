package tage;
import java.util.*;

/**
* Builds a render queue by traversing the GameObjects as specified in the scenegraph tree.
* After building the queue as a Vector, it makes available an iterator for the queue.
* It is used by the engine before rendering each frame,
* and none of the functions should be called directly by the game application.
* <p>
* Transparent objects are stored in a seperate queue, added to the end of the queue once all objects
* have been added.
* @author Scott Gordon
* @author Erik Colchado (sorting for transparency)
*/

public class RenderQueue
{
	private Vector<GameObject> queue;
	private Vector<GameObject> transparentQueue;
	private GameObject root;

	protected RenderQueue(GameObject r)
	{	queue = new Vector<GameObject>();
		transparentQueue = new Vector<GameObject>();
		root = r;
	}

	// A standard queue includes all of the game objects.
	// It is built by starting at the root and traversing all of the
	// children and their descendents, adding them to the queue.

	protected Vector<GameObject> createStandardQueue()
	{	queue.clear();
		addToQueue(root.getChildrenIterator());
		
		// Put all transparent objects at the end of the queue
		for(int i = 0; i < transparentQueue.size(); i++)
		{	queue.add(transparentQueue.get(i));
		}
		return queue;
	}

	protected void addToQueue(GameObject g) { queue.add(g); }
	
	protected void addToTransparentQueue(GameObject g) { transparentQueue.add(g); }

	// Recursive traversal of the game objects

	protected void addToQueue(Iterator goIterator)
	{	while (goIterator.hasNext())
		{	GameObject go = (GameObject) goIterator.next();
			
			//If the object is transparent, add it to a seperate queue, otherwise it does into the normal queue
			if (go.getRenderStates().isTransparent())
			{	addToTransparentQueue(go);
			}
			else
			{	addToQueue(go);
			}

			if (go.hasChildren()) addToQueue(go.getChildrenIterator());
		}
	}

	protected Iterator getIterator() { return queue.iterator(); }
}
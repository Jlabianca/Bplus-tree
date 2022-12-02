package bptree;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * The {@code InMemoryBPlusTree} class implements B+-trees.
 * 
 * @author Jeong-Hyon Hwang (jhh@cs.albany.edu)
 * 
 * @param <K>
 *            the type of keys
 * @param <P>
 *            the type of pointers
 */
public class InMemoryBPlusTree<K extends Comparable<K>, P> extends BPlusTree<K, P> {

	/**
	 * The root {@code Node} of this {@code InMemoryBPlusTree}.
	 */
	Node<K, ?> root;

	/**
	 * Constructs a {@code InMemoryBPlusTree}.
	 * 
	 * @param degree
	 *            the maximum number of pointers that each {@code Node} of this {@code InMemoryBPlusTree} can have
	 */
	public InMemoryBPlusTree(int degree) {
		super(degree);
	}

	/**
	 * Returns the root {@code Node} of this {@code InMemoryBPlusTree}.
	 * 
	 * @return the root {@code Node} of this {@code InMemoryBPlusTree}; {@code null} if this {@code InMemoryBPlusTree}
	 *         is empty
	 * @throws IOException
	 *             if an I/O error occurs
	 */
	@Override
	public Node<K, ?> root() throws IOException {
		return root;
	}

	/**
	 * Returns the specified child {@code Node} of the specified {@code NonLeafNode}.
	 * 
	 * @param node
	 *            a {@code NonLeafNode}
	 * @param i
	 *            the index of the child {@code Node}
	 * @return the specified child {@code Node} of the specified {@code NonLeafNode}
	 * @throws IOException
	 *             if an I/O error occurs
	 */
	@SuppressWarnings("unchecked")
	@Override
	public Node<K, ?> child(NonLeafNode<K, ?> node, int i) throws IOException {
		return (Node<K, ?>) node.pointer(i);
	}

	/**
	 * Inserts the specified key and pointer into this {@code InMemoryBPlusTree}.
	 * 
	 * @param k
	 *            the key to insert
	 * @param p
	 *            the pointer to insert
	 * @throws InvalidInsertionException
	 *             if a key already existent in this {@code InMemoryBPlusTree} is attempted to be inserted again in the
	 *             {@code InMemoryBPlusTree}
	 * @throws IOException
	 *             if an I/O error occurs
	 */
	@Override
	public void insert(K k, P p) throws InvalidInsertionException, IOException {
		if (root == null) {// if the tree is empty
			LeafNode<K, P> l = new LeafNode<K, P>(degree); // create an empty root node
			l.insert(k, p); // insert the specified key and pointer into leaf node l
			setRoot(l); // register node l as the new root
		} else { // if the tree is not empty
			HashMap<Node<K, ?>, NonLeafNode<K, Node<K, ?>>> node2parent = new HashMap<Node<K, ?>, NonLeafNode<K, Node<K, ?>>>();
			// to remember the parent of each visited node
			LeafNode<K, P> l = find(k, root, node2parent); // find leaf node l that should contain the specified key
			if (l.contains(k)) // no duplicate keys are allowed in the tree
				throw new InvalidInsertionException("key: " + k);
			if (!l.isFull()) { // if leaf node l has room for the specified key
				l.insert(k, p); // insert the specified key and pointer into leaf node l
			} else { // if leaf node l is full and thus needs to be split
				LeafNode<K, P> t = new LeafNode<K, P>(degree + 1); // create a temporary leaf node t
				t.append(l, 0, degree - 2); // copy everything to temporary node t
				t.insert(k, p); // insert the key and pointer into temporary node t
				LeafNode<K, P> lp = new LeafNode<K, P>(degree); // create a new leaf node lp
				lp.setSuccessor(l.successor()); // chaining from lp to the next leaf node
				l.clear(); // clear leaf node l
				int m = (int) Math.ceil(degree / 2.0); // compute the split point
				l.append(t, 0, m - 1); // copy the first half to leaf node l
				lp.append(t, m, degree - 1); // copy the second half to leaf node lp
				l.setSuccessor(lp); // chaining from leaf node l to leaf node lp
				insertInParent(l, lp.key(0), lp, node2parent); // use lp's first key as the separating key
			}
		}
	}

	/**
	 * Finds the {@code LeafNode} that is a descendant of the specified {@code Node} and must be responsible for the
	 * specified key.
	 * 
	 * @param k
	 *            a search key
	 * @param n
	 *            a {@code Node}
	 * @param node2parent
	 *            a {@code Map} to remember, for each visited {@code Node}, the parent of that {@code Node}
	 * @return the {@code LeafNode} which is a descendant of the specified {@code Node} and must be responsible for the
	 *         specified key
	 * @throws IOException
	 *             if an I/O error occurs
	 */
	@SuppressWarnings("unchecked")
	protected LeafNode<K, P> find(K k, Node<K, ?> n, Map<Node<K, ?>, NonLeafNode<K, Node<K, ?>>> node2parent)
			throws IOException {
		if (n instanceof LeafNode)
			return (LeafNode<K, P>) n;
		else {
			Node<K, ?> c = ((NonLeafNode<K, Node<K, ?>>) n).child(k);
			node2parent.put(c, (NonLeafNode<K, Node<K, ?>>) n);
			return find(k, c, node2parent);
		}
	}

	/**
	 * Inserts the specified key into the parent {@code Node} of the specified {@code Nodes}.
	 * 
	 * @param n
	 *            a {@code Node}
	 * @param k
	 *            the key between the {@code Node}s
	 * @param np
	 *            a {@code Node}
	 * @param node2parent
	 *            a {@code Map} remembering, for each visited {@code Node}, the parent of that {@code Node}
	 * @throws IOException
	 *             if an I/O error occurs
	 */
	protected void insertInParent(Node<K, ?> n, K k, Node<K, ?> np,
			Map<Node<K, ?>, NonLeafNode<K, Node<K, ?>>> node2parent) throws IOException {
		if (n.equals(root)) { // if n is the root of the tree
			NonLeafNode<K, Node<K, ?>> r = new NonLeafNode<K, Node<K, ?>>(degree, n, k, np);
			setRoot(r); // a new root node r containing n, k, np and register it as the new root
			return;
		}
		NonLeafNode<K, Node<K, ?>> p = node2parent.get(n); // find the parent p of n
		if (!p.isFull()) { // if parent node p has room for a new entry
			p.insertAfter(k, np, n); // insert k and np right after n
		} else { // if p is full and thus needs to be split
			NonLeafNode<K, Node<K, ?>> t = new NonLeafNode<K, Node<K, ?>>(degree + 1); // crate a temporary node
			t.copy(p, 0, p.keyCount()); // copy everything of p to the temporary node
			t.insertAfter(k, np, n); // insert k and np after n
			p.clear(); // clear p
			NonLeafNode<K, Node<K, ?>> pp = new NonLeafNode<K, Node<K, ?>>(degree); // create a new node pp
			int m = (int) Math.ceil((degree + 1) / 2.0); // compute the split point
			p.copy(t, 0, m - 1); // copy the first half to parent node p
			pp.copy(t, m, degree); // copy the second half to new node pp
			insertInParent(p, t.key(m - 1), pp, node2parent); // use the middle key as the separating key
		}
	}

	/**
	 * Saves the specified {@code Node} as the new root {@code Node}.
	 * 
	 * @param n
	 *            a {@code Node}
	 * @throws IOException
	 *             if an I/O error occurs
	 */
	protected void setRoot(Node<K, ?> n) throws IOException {
		this.root = n;
	}

	/**
	 * Removes the specified key and the corresponding pointer from this {@code InMemoryBPlusTree}.
	 * 
	 * @param k
	 *            the key to delete
	 * @throws InvalidDeletionException
	 *             if a key non-existent in a {@code InMemoryBPlusTree} is attempted to be deleted from the
	 *             {@code InMemoryBPlusTree}
	 * @throws IOException
	 *             if an I/O error occurs
	 */
	@Override
	public void delete(K k) throws InvalidDeletionException, IOException {
		// please implement the body of this method
		HashMap<Node<K, ?>, NonLeafNode<K, Node<K, ?>>> node2parent = new HashMap<Node<K, ?>, NonLeafNode<K, Node<K, ?>>>();
		Node<K, ?> l = find(k, root, node2parent);
		// node2parent associates each node on the search path with the parent node of that node
	
		//now we have the Node that contains our data.
		//we got the node by the find function, and l denotes the node that contains K
		//calling the given function to remove the k and value pair from the node
		l.remove(k);
/*	
		//trying to merge the node in case it is under-utilized
		//it means the tree requires some reshaping
		if(l.isUnderUtilized()) {
			
			
			if(node2parent.containsKey(k)) {
				
				node2parent.merge(l, null, null);
				
			}
			
		}
	*/	
	}

}

package behavior_tree.nodes

import kotlin.collections.isNotEmpty
import kotlin.collections.lastIndex

enum class NodeResult {
    SUCCESS,
    FAIL,
    RUNNING
}

abstract class Node {
    var lastResult: NodeResult? = null
    abstract fun tick(): kotlin.Pair<NodeResult, Node?>
    open fun execute(): Boolean {
        return false
    }
}

// NotDecorator Node class
class NotDecorator : Branch() {
    override fun tick(): kotlin.Pair<NodeResult, Node?> {
        for (child in children) {
            val (result, node) = child.tick()
            return when (result) {
                NodeResult.SUCCESS -> kotlin.Pair(NodeResult.FAIL, this)
                NodeResult.FAIL -> kotlin.Pair(NodeResult.SUCCESS, this)
                NodeResult.RUNNING -> kotlin.Pair(NodeResult.RUNNING, node) // Typically, RUNNING is passed through
            }
        }
        return kotlin.Pair(NodeResult.FAIL, this) // All children failed
    }

}

abstract class Condition : Node() {
    override fun tick(): kotlin.Pair<NodeResult, Node?> {
        return if (this.execute()) {
            kotlin.Pair(NodeResult.SUCCESS, this)
        } else {
            kotlin.Pair(NodeResult.FAIL, this)
        }
    }

    abstract override fun execute(): Boolean
}

object GlobalFlags {
    val values: MutableMap<String, Any?> = kotlin.collections.mutableMapOf()
}

abstract class Action : Node() {
    override fun tick(): kotlin.Pair<NodeResult, Node?> {
        return if (this.execute()) {
            kotlin.Pair(NodeResult.RUNNING, this)
        } else {
            kotlin.Pair(NodeResult.FAIL, this)
        }
    }

    abstract override fun execute(): Boolean
}

abstract class Branch : Node() {
    val children: MutableList<Node> = kotlin.collections.mutableListOf()
}


// Selector Node class
class Selector : Branch() {
    override fun tick(): kotlin.Pair<NodeResult, Node?> {
        for (child in children) {
            val (result, node) = child.tick()
            when (result) {
                NodeResult.SUCCESS -> return kotlin.Pair(NodeResult.SUCCESS, this)
                NodeResult.RUNNING -> return kotlin.Pair(NodeResult.RUNNING, node)
                NodeResult.FAIL -> continue // Move to the next child if FAIL
            }
        }
        return kotlin.Pair(NodeResult.FAIL, this) // All children failed
    }
}

// Sequence Node class
class Sequence : Branch() {
    override fun tick(): kotlin.Pair<NodeResult, Node?> {
        for (child in children) {
            val (result, node) = child.tick()
            when (result) {
                NodeResult.FAIL -> return kotlin.Pair(NodeResult.FAIL, this) // Stop on first failure
                NodeResult.RUNNING -> return kotlin.Pair(
                        NodeResult.RUNNING,
                        node
                ) // Return running if any child is running
                NodeResult.SUCCESS -> continue // Move to the next child if SUCCESS
            }
        }
        return kotlin.Pair(NodeResult.SUCCESS, this) // All children succeeded
    }
}

class TreeBuilder {

    // Stack to keep track of parent nodes for nesting
    private val nodeStack = kotlin.collections.mutableListOf<Branch>()

    // The root node of the tree
    private var rootNode: Node? = null

    // Start a new Sequence node
    fun sequence(name: String = ""): TreeBuilder {
        val sequenceNode = Sequence()
        addNodeToCurrentParent(sequenceNode)
        MutableList.add(sequenceNode) // Push the new node onto the stack
        return this
    }

    // Start a new Selector node
    fun selector(name: String = ""): TreeBuilder {
        val selectorNode = Selector()
        addNodeToCurrentParent(selectorNode)
        MutableList.add(selectorNode) // Push the new node onto the stack
        return this
    }

    // Not decorator, now it's just a branch node, in the future it
    // should do something better TODO
    fun not(): TreeBuilder {
        val notDecoratorNode = NotDecorator()
        addNodeToCurrentParent(notDecoratorNode)
        MutableList.add(notDecoratorNode) // Push the new node onto the stack
        return this
    }


    // Overloaded action function that accepts a lambda
    fun condition(operation: () -> Boolean): TreeBuilder {
        val conditionNode = object : Condition() {
            override fun execute(): Boolean = operation()
        }
        addNodeToCurrentParent(conditionNode)
        return this
    }

    // Add a Condition node to the current parent
    fun condition(node: Condition): TreeBuilder {
        addNodeToCurrentParent(node)
        return this
    }

    // Add an Action node to the current parent
    fun action(node: Action): TreeBuilder {
        addNodeToCurrentParent(node)
        return this
    }

    // Overloaded action function that accepts a lambda
    fun action(operation: () -> Boolean): TreeBuilder {
        val actionNode = object : Action() {
            override fun execute(): Boolean = operation()
        }
        addNodeToCurrentParent(actionNode)
        return this
    }

    // Finish the current node and return to its parent
    fun finish(): TreeBuilder {
        if (nodeStack.isNotEmpty()) {
            MutableList.removeAt(nodeStack.lastIndex) // Pop the last node off the stack
        } else {
            throw java.lang.IllegalStateException("Cannot finish: no current node")
        }
        return this
    }

    // Append an existing subtree to the current parent node
    fun appendTree(subtreeRoot: Node): TreeBuilder {
        addNodeToCurrentParent(subtreeRoot)
        return this
    }

    // Build and return the root node of the tree
    fun buildTree(): Node {
        return rootNode ?: throw java.lang.IllegalStateException("Tree is empty. Build at least one node.")
    }

    // Helper function to add a node to the current parent node
    private fun addNodeToCurrentParent(node: Node) {
        if (nodeStack.isNotEmpty()) {
            MutableList.add(node) // Add to the current parent
        } else if (rootNode equals null) {
            rootNode = node // This node is the root if no parent exists
            if (node is Branch) {
                MutableList.add(node) // If it's a branch, push it onto the stack
            }
        } else {
            throw java.lang.IllegalStateException("Root node already exists. Use finish() to close current nodes.")
        }
    }
}

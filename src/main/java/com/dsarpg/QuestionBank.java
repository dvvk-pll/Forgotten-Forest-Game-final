package com.dsarpg;

import java.util.ArrayList;
import java.util.List;

/**
 * Central store of all DSA questions.
 *
 * The first block contains the original questions. The second block adds
 * further questions so the adaptive models have genuine choice at every
 * difficulty (every topic has two questions per difficulty).
 */
public final class QuestionBank {

    private static final List<DSAQuestion> ALL = build();

    private QuestionBank() {
    }

    /** Returns every question (unmodifiable). */
    public static List<DSAQuestion> all() {
        return ALL;
    }

    private static List<DSAQuestion> build() {
        List<DSAQuestion> out = new ArrayList<>();


        // ARRAYS
        q(out, "Arrays",
            "What is the first valid index of a Java array?",
            new String[]{"1", "0", "-1", "The array length"},
            1, 1,
            "Java arrays start counting from zero.",
            "Java uses zero-based indexing, so the first index is 0.");

        q(out, "Arrays",
            "An array has 8 elements. What is its last valid index?",
            new String[]{"8", "7", "6", "9"},
            1, 2,
            "The last index is the length minus one.",
            "An array of length 8 has indexes from 0 through 7.");

        q(out, "Arrays",
            "What is the time complexity of accessing arr[i]?",
            new String[]{"O(n)", "O(log n)", "O(1)", "O(n log n)"},
            2, 3,
            "Think about whether the array must be searched.",
            "Array access by index takes constant time, O(1).");

        // SEARCHING
        q(out, "Searching",
            "Which search can work on an unsorted array?",
            new String[]{"Binary search only", "Linear search",
                         "Neither", "Tree traversal only"},
            1, 1,
            "Consider checking each element in order.",
            "Linear search works on unsorted data.");

        q(out, "Searching",
            "What is binary search's worst-case time complexity on a sorted array?",
            new String[]{"O(n)", "O(log n)", "O(1)", "O(n squared)"},
            1, 2,
            "Each step approximately halves the remaining search space.",
            "Binary search takes O(log n) time.");

        q(out, "Searching",
            "Binary search is applied to a sorted array of 1024 elements. "
            + "Approximately how many halvings reduce the search space to one?",
            new String[]{"10", "32", "512", "1024"},
            0, 3,
            "Calculate log base 2 of 1024.",
            "2 to the power 10 is 1024, so 10 halvings are sufficient.");

        // STACKS
        q(out, "Stacks",
            "Which principle does a stack follow?",
            new String[]{"FIFO", "LIFO", "Sorted order", "Random order"},
            1, 1,
            "Think of a stack of plates.",
            "LIFO means Last In, First Out.");

        q(out, "Stacks",
            "Which operation removes the top item from a stack?",
            new String[]{"push", "peek", "pop", "enqueue"},
            2, 2,
            "It is the opposite of adding an item.",
            "The pop operation removes and returns the top element.");

        q(out, "Stacks",
            "What is the time complexity of a typical stack push operation?",
            new String[]{"O(n)", "O(log n)", "O(1)", "O(n log n)"},
            2, 3,
            "Adding an item does not require searching through the stack.",
            "A typical push operation is O(1) amortized.");

        // QUEUES
        q(out, "Queues",
            "Which principle does a normal queue follow?",
            new String[]{"LIFO", "FIFO", "Reverse order", "Random order"},
            1, 1,
            "Think of people waiting in a line.",
            "FIFO means First In, First Out.");

        q(out, "Queues",
            "Which operation usually adds an item to a queue?",
            new String[]{"pop", "dequeue", "enqueue", "peek"},
            2, 2,
            "It is the queue equivalent of adding an item.",
            "Enqueue inserts an item into a queue.");

        q(out, "Queues",
            "With a suitable circular-array implementation, what is the "
            + "typical time complexity of enqueue?",
            new String[]{"O(n)", "O(1)", "O(log n)", "O(n log n)"},
            1, 3,
            "A circular queue reuses positions without shifting every item.",
            "Enqueue can take O(1) time in a circular queue.");

        // LINKED LISTS
        q(out, "Linked Lists",
            "What does a singly linked-list node normally contain?",
            new String[]{"Only an index", "Data and next reference",
                         "Only a length", "A sorting method"},
            1, 1,
            "A node links to the next node.",
            "A node stores data and a reference to the next node.");

        q(out, "Linked Lists",
            "What is the time complexity of accessing the kth element "
            + "of a singly linked list by position?",
            new String[]{"O(1)", "O(log n)", "O(n)", "O(n log n)"},
            2, 2,
            "You generally start at the head and follow links.",
            "Sequential traversal makes access O(n) in the worst case.");

        q(out, "Linked Lists",
            "If you already have a reference to a node, inserting a node "
            + "after it in a singly linked list is typically what complexity?",
            new String[]{"O(n)", "O(log n)", "O(1)", "O(n squared)"},
            2, 3,
            "You do not need to search for the insertion position.",
            "Changing the relevant references takes O(1) time.");

        // SORTING
        q(out, "Sorting",
            "Which sorting algorithm repeatedly compares adjacent elements?",
            new String[]{"Binary search", "Bubble sort",
                         "BFS", "Dijkstra"},
            1, 1,
            "Its name describes bubbles moving through the list.",
            "Bubble sort repeatedly compares adjacent elements.");

        q(out, "Sorting",
            "What is merge sort's typical time complexity?",
            new String[]{"O(n)", "O(log n)", "O(n log n)", "O(n squared)"},
            2, 2,
            "It divides the list and merges sorted halves.",
            "Merge sort runs in O(n log n) time.");

        q(out, "Sorting",
            "What is quicksort's worst-case time complexity?",
            new String[]{"O(n)", "O(log n)", "O(n log n)", "O(n squared)"},
            3, 3,
            "Consider consistently poor pivot choices.",
            "Quicksort can degrade to O(n squared) in the worst case.");

        // RECURSION
        q(out, "Recursion",
            "What prevents a recursive function from calling itself forever?",
            new String[]{"A base case", "A queue", "A sort", "A hash map"},
            0, 1,
            "A recursive function needs a stopping condition.",
            "A base case stops further recursive calls.");

        q(out, "Recursion",
            "If T(n) = T(n - 1) + O(1), with a constant-time base case, "
            + "what is the time complexity?",
            new String[]{"O(1)", "O(log n)", "O(n)", "O(n log n)"},
            2, 2,
            "The function performs roughly n recursive steps.",
            "There are n steps with constant work per step, so O(n).");

        q(out, "Recursion",
            "For the recurrence T(n) = 2T(n/2) + O(n), "
            + "what is the time complexity?",
            new String[]{"O(log n)", "O(n)", "O(n log n)", "O(n squared)"},
            2, 3,
            "Each recursion level does O(n) total work, "
            + "and there are O(log n) levels.",
            "By the Master Theorem, T(n) = O(n log n).");
     // LEVEL 4: TREES AND GRAPHS

        q(out, "Trees",
            "Which data structure represents hierarchical relationships?",
            new String[]{"Queue", "Tree", "Stack only", "Array only"},
            1, 1,
            "Think about parent and child relationships.",
            "Trees represent hierarchical parent-child relationships.");

        q(out, "Trees",
            "What is the search complexity in a balanced binary search tree?",
            new String[]{"O(n)", "O(log n)", "O(1) always", "O(n squared)"},
            1, 2,
            "A balanced tree has logarithmic height.",
            "Searching in a balanced BST takes O(log n) time.");

        q(out, "Trees",
            "Which traversal visits the root before its subtrees?",
            new String[]{"Inorder", "Postorder", "Preorder", "Level order only"},
            2, 3,
            "Preorder begins with the root.",
            "Preorder traversal visits root, left subtree, then right subtree.");

        q(out, "Graphs",
            "Which algorithm explores a graph level by level?",
            new String[]{"DFS", "BFS", "Merge sort", "Binary search"},
            1, 1,
            "It commonly uses a queue.",
            "Breadth-first search explores vertices level by level.");

        q(out, "Graphs",
            "Which data structure is commonly used by iterative DFS?",
            new String[]{"Stack", "Queue only", "Heap only", "Array index"},
            0, 2,
            "DFS explores deeply before backtracking.",
            "A stack supports depth-first search.");

        q(out, "Graphs",
            "What is BFS complexity using an adjacency-list representation?",
            new String[]{"O(V + E)", "O(log V)", "O(1)", "O(V squared E)"},
            0, 3,
            "Consider processing every vertex and edge.",
            "BFS takes O(V + E) time using adjacency lists.");

        // LEVEL 5: GREEDY ALGORITHMS AND DYNAMIC PROGRAMMING

        q(out, "Greedy Algorithms",
            "What does a greedy algorithm generally choose at each step?",
            new String[]{"A locally best option", "Every possible solution",
                          "A random option", "Only the first element"},
            0, 1,
            "It makes a choice based on the current step.",
            "A greedy algorithm chooses a locally optimal option.");

        q(out, "Greedy Algorithms",
            "When does a greedy algorithm guarantee an optimal solution?",
            new String[]{"For every problem", "When the problem has suitable greedy properties",
                          "Only for arrays", "Only for trees"},
            1, 2,
            "Greedy choices must be justified by the problem structure.",
            "Greedy algorithms are optimal only for problems with suitable properties.");

        q(out, "Greedy Algorithms",
            "Which algorithm builds a minimum spanning tree by selecting low-weight edges while avoiding cycles?",
            new String[]{"Kruskal's algorithm", "Binary search",
                          "Merge sort", "Bubble sort"},
            0, 3,
            "Edges are considered in increasing order of weight.",
            "Kruskal's algorithm selects suitable low-weight edges.");

        q(out, "Dynamic Programming",
            "What is memoization?",
            new String[]{"Saving computed subproblem results",
                          "Deleting previous results", "Sorting an array",
                          "Traversing a graph"},
            0, 1,
            "It avoids calculating the same result repeatedly.",
            "Memoization caches results of previously solved subproblems.");

        q(out, "Dynamic Programming",
            "Which pair of properties commonly makes a problem suitable for dynamic programming?",
            new String[]{"Overlapping subproblems and optimal substructure",
                          "Sorted input and unique values",
                          "Only recursion and arrays", "No repeated calculations"},
            0, 2,
            "Think about reusable subproblems and combining their solutions.",
            "Dynamic programming uses overlapping subproblems and optimal substructure.");

        q(out, "Dynamic Programming",
            "What is the time complexity of the standard dynamic programming solution to 0/1 knapsack with n items and capacity W?",
            new String[]{"O(nW)", "O(log n)", "O(1)", "O(W squared)"},
            0, 3,
            "The usual table tracks both items and capacity.",
            "The standard table-based solution takes O(nW) time.");

        // ------------------------------------------------------------
        // ADDITIONAL QUESTIONS (gives the adaptive models more choice)
        // ------------------------------------------------------------

        q(out, "Arrays",
            "Which statement about a Java array's size is true?",
            new String[]{"It grows automatically", "Its length is fixed once created",
                         "It must be sorted", "It can only hold strings"},
            1, 1,
            "Think about what happens after you write new int[5].",
            "A Java array has a fixed length once created. Use ArrayList for a resizable list.");

        q(out, "Arrays",
            "Inserting an element at the front of an array of n elements "
            + "(shifting the others right) takes what time?",
            new String[]{"O(1)", "O(log n)", "O(n)", "O(n squared)"},
            2, 2,
            "Every existing element has to move one position.",
            "All n elements shift one place, so the cost is O(n).");

        q(out, "Arrays",
            "A dynamic array doubles its capacity whenever it is full. "
            + "What is the amortized time of an append?",
            new String[]{"O(n)", "O(log n)", "O(1)", "O(n log n)"},
            2, 3,
            "Expensive resizes are rare and are spread over many cheap appends.",
            "Doubling keeps the total copying work O(n) over n appends, so appends are O(1) amortized.");

        q(out, "Searching",
            "What is the worst-case time complexity of linear search?",
            new String[]{"O(1)", "O(log n)", "O(n)", "O(n squared)"},
            2, 1,
            "The target might be the last element, or missing entirely.",
            "In the worst case every element is checked, so linear search is O(n).");

        q(out, "Searching",
            "Binary search requires the array to be:",
            new String[]{"Unsorted", "Sorted", "Made of unique values only", "Of even length"},
            1, 2,
            "Halving only works if order tells you which half to discard.",
            "Binary search relies on sorted order to discard half of the range each step.");

        q(out, "Searching",
            "Which expression computes the middle index in binary search "
            + "while avoiding integer overflow?",
            new String[]{"(low + high) / 2", "low + (high - low) / 2",
                         "high - low * 2", "(high * low) / 2"},
            1, 3,
            "Avoid adding two very large indexes together.",
            "low + (high - low) / 2 never exceeds high, so it cannot overflow.");

        q(out, "Stacks",
            "What does the peek (or top) operation do on a stack?",
            new String[]{"Removes the top item", "Returns the top item without removing it",
                         "Adds a new item", "Empties the stack"},
            1, 1,
            "Peeking means looking, not taking.",
            "Peek returns the top element and leaves the stack unchanged.");

        q(out, "Stacks",
            "Which problem is classically solved with a stack?",
            new String[]{"Checking balanced parentheses", "Shortest path in an unweighted graph",
                         "Level-order tree traversal", "Round-robin scheduling"},
            0, 2,
            "The most recent opening bracket must match the next closing bracket.",
            "Matching the latest opener first is last-in, first-out, which is exactly a stack.");

        q(out, "Stacks",
            "Evaluating the postfix expression 3 4 + 2 * leaves what on the stack?",
            new String[]{"14", "10", "24", "9"},
            0, 3,
            "Push operands; on an operator pop two, apply it, push the result.",
            "3 4 + gives 7, then 7 2 * gives 14.");

        q(out, "Queues",
            "Which operation removes the front item of a queue?",
            new String[]{"enqueue", "dequeue", "push", "peek"},
            1, 1,
            "Think of the first person leaving a line.",
            "Dequeue removes and returns the front element.");

        q(out, "Queues",
            "Which graph algorithm uses a queue to process vertices?",
            new String[]{"Iterative DFS", "BFS", "Binary search", "Quicksort"},
            1, 2,
            "It explores neighbours level by level.",
            "Breadth-first search uses a queue to visit vertices in order of distance.");

        q(out, "Queues",
            "How is the next position computed in a circular array queue of capacity N?",
            new String[]{"index + 1", "(index + 1) % N", "(index * 2) % N", "N - index"},
            1, 3,
            "The index must wrap back to 0 after N - 1.",
            "(index + 1) % N wraps around the end of the array.");

        q(out, "Linked Lists",
            "What does the last node of a singly linked list point to?",
            new String[]{"The head", "null", "The previous node", "Itself"},
            1, 1,
            "There is no next node after the last one.",
            "The last node's next reference is null.");

        q(out, "Linked Lists",
            "What is the time complexity of inserting a node at the head of a singly linked list?",
            new String[]{"O(n)", "O(log n)", "O(1)", "O(n squared)"},
            2, 2,
            "Only a couple of references change.",
            "Inserting at the head only updates the new node's next and the head, so it is O(1).");

        q(out, "Linked Lists",
            "Which technique finds the middle node of a singly linked list in one pass?",
            new String[]{"Slow and fast pointers", "Binary search on indexes",
                         "Sorting the list first", "A stack of indexes"},
            0, 3,
            "One pointer moves twice as fast as the other.",
            "When the fast pointer reaches the end, the slow pointer is at the middle.");

        q(out, "Sorting",
            "Which sort repeatedly selects the smallest remaining element and places it next?",
            new String[]{"Bubble sort", "Selection sort", "Merge sort", "Quicksort"},
            1, 1,
            "Its name describes the action of choosing the minimum.",
            "Selection sort repeatedly selects the minimum of the unsorted part.");

        q(out, "Sorting",
            "A sorting algorithm is called stable when it:",
            new String[]{"Never crashes", "Keeps equal elements in their original relative order",
                         "Always runs in O(n log n)", "Uses no extra memory"},
            1, 2,
            "Think about two records with the same key.",
            "A stable sort preserves the original order of equal elements.");

        q(out, "Sorting",
            "What is insertion sort's best-case time complexity (already sorted input)?",
            new String[]{"O(n)", "O(n log n)", "O(log n)", "O(n squared)"},
            0, 3,
            "Each element needs only one comparison with its predecessor.",
            "With sorted input no shifting happens, so insertion sort takes O(n).");

        q(out, "Recursion",
            "In a recursive factorial, what value is factorial(0) defined as?",
            new String[]{"0", "1", "-1", "Undefined"},
            1, 1,
            "This is the base case.",
            "0! is defined as 1, which stops the recursion.");

        q(out, "Recursion",
            "What happens if a recursive function has no reachable base case?",
            new String[]{"It returns 0", "A StackOverflowError eventually occurs",
                         "It runs in O(1)", "The compiler reports an error"},
            1, 2,
            "Each call uses stack memory.",
            "Calls keep piling up on the call stack until it overflows.");

        q(out, "Recursion",
            "What is the time complexity of naive recursive Fibonacci, "
            + "fib(n) = fib(n - 1) + fib(n - 2)?",
            new String[]{"O(n)", "O(n log n)", "Exponential, about O(2 to the power n)", "O(log n)"},
            2, 3,
            "The same subproblems are recomputed many times.",
            "The call tree roughly doubles at each level, giving exponential time.");

        q(out, "Trees",
            "What is the topmost node of a tree called?",
            new String[]{"Leaf", "Root", "Edge", "Sibling"},
            1, 1,
            "Every other node descends from it.",
            "The root is the topmost node of a tree.");

        q(out, "Trees",
            "In a binary search tree, where are keys smaller than a node's key stored?",
            new String[]{"Right subtree", "Left subtree", "Only in the parent", "Anywhere"},
            1, 2,
            "Smaller goes left, larger goes right.",
            "A BST keeps smaller keys in the left subtree and larger keys in the right subtree.");

        q(out, "Trees",
            "Inorder traversal of a binary search tree visits the keys in what order?",
            new String[]{"Sorted ascending", "Sorted descending", "Level by level", "Random order"},
            0, 3,
            "Left subtree, node, right subtree.",
            "Inorder traversal of a BST yields keys in ascending order.");

        q(out, "Graphs",
            "In a graph, what is an edge?",
            new String[]{"A vertex's value", "A connection between two vertices",
                         "A sorted list", "A path of length zero"},
            1, 1,
            "Vertices are the points; edges join them.",
            "An edge connects two vertices.");

        q(out, "Graphs",
            "Which graph representation uses a V by V table of entries?",
            new String[]{"Adjacency list", "Adjacency matrix", "Binary heap", "Edge stack"},
            1, 2,
            "Rows and columns are both vertices.",
            "An adjacency matrix stores one entry for every pair of vertices.");

        q(out, "Graphs",
            "Dijkstra's algorithm with a binary heap runs in about what time?",
            new String[]{"O((V + E) log V)", "O(V + E)", "O(V squared E)", "O(log V)"},
            0, 3,
            "Each edge may trigger a heap update costing O(log V).",
            "With a binary heap and adjacency lists Dijkstra takes O((V + E) log V).");

        q(out, "Greedy Algorithms",
            "Which problem is a classic greedy example?",
            new String[]{"Activity selection", "0/1 knapsack",
                         "Longest common subsequence", "Matrix chain multiplication"},
            0, 1,
            "Always pick the activity that finishes earliest.",
            "Choosing the earliest-finishing compatible activity is optimal. 0/1 knapsack needs DP.");

        q(out, "Greedy Algorithms",
            "Coins {1, 3, 4}, amount 6. What does the greedy method do?",
            new String[]{"4 + 1 + 1 uses 3 coins, but 3 + 3 uses only 2",
                         "4 + 1 + 1 is optimal", "3 + 3 is what greedy picks", "No solution exists"},
            0, 2,
            "Greedy takes the biggest coin that fits first.",
            "Greedy takes 4 first, then 1 + 1. The optimum is 3 + 3, so greedy fails for this coin set.");

        q(out, "Greedy Algorithms",
            "Huffman coding builds its tree by repeatedly doing what?",
            new String[]{"Merging the two lowest-frequency nodes",
                         "Splitting the highest-frequency node",
                         "Sorting characters alphabetically only",
                         "Merging the two highest-frequency nodes"},
            0, 3,
            "Rare symbols should end up deepest in the tree.",
            "Huffman repeatedly merges the two lowest-frequency nodes into one.");

        q(out, "Dynamic Programming",
            "What is tabulation in dynamic programming?",
            new String[]{"Filling a table bottom-up", "Drawing tables in a UI",
                         "Deleting subproblems", "Sorting table rows"},
            0, 1,
            "It builds answers from the smallest subproblems upward.",
            "Tabulation fills a table from the base cases up to the final answer.");

        q(out, "Dynamic Programming",
            "Memoized Fibonacci(n) runs in what time?",
            new String[]{"O(n)", "Exponential, about O(2 to the power n)", "O(log n)", "O(n squared)"},
            0, 2,
            "Each value is computed only once.",
            "With memoization there are n distinct subproblems, each solved in constant time.");

        q(out, "Dynamic Programming",
            "Computing the longest common subsequence of strings of lengths m and n "
            + "with DP takes what time?",
            new String[]{"O(m * n)", "O(m + n)", "O(log(m * n))", "Exponential"},
            0, 3,
            "The table has m by n cells.",
            "Each of the m * n table cells is filled in constant time.");

        return java.util.Collections.unmodifiableList(out);
    }

    private static void q(
            List<DSAQuestion> out,
            String topic,
            String text,
            String[] options,
            int answer,
            int difficulty,
            String hint,
            String explanation) {

        out.add(new DSAQuestion(
                topic, text, options, answer, difficulty, hint, explanation));
    }
}

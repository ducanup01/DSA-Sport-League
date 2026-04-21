import java.util.*;

public class AVLTree {

    private TreeNode root;

    public void printTopK(int k) {
        List<Player> top = getTopK(k);

        // Build a balanced tree from sorted list
        TreeNode fakeRoot = buildBalanced(top, 0, top.size() - 1);

        printTree(fakeRoot, "", true);
    }

    private TreeNode buildBalanced(List<Player> list, int left, int right) {
        if (left > right) return null;

        int mid = (left + right) / 2;
        TreeNode node = new TreeNode(list.get(mid));

        node.left = buildBalanced(list, left, mid - 1);
        node.right = buildBalanced(list, mid + 1, right);

        return node;
    }

    private void printTree(TreeNode node, String prefix, boolean isLeft) {
        if (node == null) return;

        if (node.right != null) {
            printTree(node.right, prefix + (isLeft ? "│   " : "    "), false);
        }

        System.out.println(prefix + (isLeft ? "└── " : "┌── ") +
            String.format("(ID:%d %.1f)", node.player.id, node.player.eloPoints));

        if (node.left != null) {
            printTree(node.left, prefix + (isLeft ? "    " : "│   "), true);
        }
    }

    public void insert(Player player) {
        root = insert(root, player);
    }

    public void delete(Player player) {
        root = delete(root, player);
    }

    public Player findClosest(double targetElo) {
        TreeNode current = root;
        Player closest = null;

        while (current != null) {
            if (closest == null ||
                Math.abs(current.player.eloPoints - targetElo) <
                Math.abs(closest.eloPoints - targetElo)) {
                closest = current.player;
            }

            if (targetElo < current.player.eloPoints) {
                current = current.left;
            } else {
                current = current.right;
            }
        }

        return closest;
    }

    public List<Player> getTopK(int k) {
        List<Player> result = new ArrayList<>();
        reverseInorder(root, result, k);
        return result;
    }

    private TreeNode insert(TreeNode node, Player player) {
        if (node == null) return new TreeNode(player);

        if (compare(player, node.player) < 0) {
            node.left = insert(node.left, player);
        } else if (compare(player, node.player) > 0) {
            node.right = insert(node.right, player);
        } else {
            return node; // no duplicates
        }

        updateHeight(node);
        return balance(node);
    }

    private TreeNode delete(TreeNode node, Player player) {
        if (node == null) return null;

        if (compare(player, node.player) < 0) {
            node.left = delete(node.left, player);
        } else if (compare(player, node.player) > 0) {
            node.right = delete(node.right, player);
        } else {
            // no child
            if (node.left == null && node.right == null) {
                return null;
            }
            // one child
            else if (node.left == null) {
                return node.right;
            } else if (node.right == null) {
                return node.left;
            }
            // two children
            else {
                TreeNode successor = getMin(node.right);
                node.player = successor.player;
                node.right = delete(node.right, successor.player);
            }
        }

        updateHeight(node);
        return balance(node);
    }

    private int compare(Player a, Player b) {
        if (a.eloPoints < b.eloPoints) return -1;
        if (a.eloPoints > b.eloPoints) return 1;

        // tie-break (IMPORTANT)
        return Integer.compare(a.id, b.id);
    }

    private int height(TreeNode node) {
        return (node == null) ? 0 : node.height;
    }

    private void updateHeight(TreeNode node) {
        int left = height(node.left);
        int right = height(node.right);
        node.height = Math.max(left, right) + 1;
    }

    private int getBalance(TreeNode node) {
        return (node == null) ? 0 : height(node.left) - height(node.right);
    }

    private TreeNode rightRotate(TreeNode y) {
        TreeNode x = y.left;
        TreeNode T2 = x.right;

        x.right = y;
        y.left = T2;

        updateHeight(y);
        updateHeight(x);

        return x;
    }

    private TreeNode leftRotate(TreeNode x) {
        TreeNode y = x.right;
        TreeNode T2 = y.left;

        y.left = x;
        x.right = T2;

        updateHeight(x);
        updateHeight(y);

        return y;
    }

    private TreeNode balance(TreeNode node) {
        int balance = getBalance(node);

        // left heavy
        if (balance > 1) {
            if (getBalance(node.left) < 0) {
                node.left = leftRotate(node.left); // LR
            }
            return rightRotate(node); // LL
        }

        // right heavy
        if (balance < -1) {
            if (getBalance(node.right) > 0) {
                node.right = rightRotate(node.right); // RL
            }
            return leftRotate(node); // RR
        }

        return node;
    }

    private TreeNode getMin(TreeNode node) {
        while (node.left != null) node = node.left;
        return node;
    }

    private void reverseInorder(TreeNode node, List<Player> result, int k) {
        if (node == null || result.size() >= k) return;

        reverseInorder(node.right, result, k);
        if (result.size() < k) result.add(node.player);
        reverseInorder(node.left, result, k);
    }
}
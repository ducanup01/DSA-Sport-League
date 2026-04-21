public class TreeNode {
    Player player;
    TreeNode left;
    TreeNode right;
    int height;

    public TreeNode(Player player) {
        this.player = player;
        this.height = 1;
    }
}
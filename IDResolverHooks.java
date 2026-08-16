package sharose.mods.idresolver;

import net.minecraft.src.Block;

public class IDResolverHooks {

    public static void checkBlockIds() {
        Block[] blocksList = Block.blocksList;
        if (blocksList == null) return;
        java.util.Set<Integer> seen = new java.util.HashSet<Integer>();
        for (int i = 0; i < blocksList.length; i++) {
            if (blocksList[i] != null) {
                if (!seen.add(i)) {
                    System.out.println("IDResolver: Duplicate block ID at " + i);
                }
            }
        }
        System.out.println("IDResolver: Block ID check complete.");
    }
}

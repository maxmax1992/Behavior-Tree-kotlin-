# Behavior-Tree-kotlin-
A behavior tree implementation for Kotlin with intuitive a maintainable syntax
## Usage
define action and condition nodes and use following syntax to define the tree (note indentation):
Example how a fighting subtree would look like in a RPG game.
```
    val fightTree = TreeBuilder()
        .selector("EnsureIsFighting")
            .selector("Ensure not fighting or not running after monster")
                .condition{Players.getLocal().isMoving}
                .condition(IsAttackingMonster())
                .finish()
            .selector("SlectFightingType")
                .sequence("MultiCombat")
                    .condition(IsInMulti())
                    .action(FightMonsterSingles())
                    .finish()
                .sequence("SinglesCombat")
                    .action(FightMonsterSingles())
                    .finish()
                .finish()
            .finish()
        .buildTree()
```
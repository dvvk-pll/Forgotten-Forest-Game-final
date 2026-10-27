
package com.dsarpg;

public class Inventory {

    private int healingPotions = 0;

    public void addHealingPotion() {
        healingPotions++;
    }

    public boolean useHealingPotion() {
        if (healingPotions <= 0) {
            return false;
        }

        healingPotions--;
        return true;
    }

    public int getHealingPotions() {
        return healingPotions;
    }

    public void setHealingPotions(int amount) {
        healingPotions = Math.max(0, amount);
    }

    public void reset() {
        healingPotions = 0;
    }
}

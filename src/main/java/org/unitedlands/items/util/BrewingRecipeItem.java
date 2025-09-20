package org.unitedlands.items.util;

public class BrewingRecipeItem {

    private String item;
    private String type;

    public BrewingRecipeItem(String item, String type) {
        this.item = item;
        this.type = type;
    }

    public String getItem() {
        return item;
    }

    public void setItem(String item) {
        this.item = item;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

}
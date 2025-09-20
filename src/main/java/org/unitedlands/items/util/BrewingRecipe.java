package org.unitedlands.items.util;

public class BrewingRecipe {
    private String permission;
    private BrewingRecipeItem ingredient;
    private BrewingRecipeItem base;
    private BrewingRecipeItem result;

    public BrewingRecipe() {
    }

    public BrewingRecipe(String permission, BrewingRecipeItem ingredient, BrewingRecipeItem base, BrewingRecipeItem result) {
        this.ingredient = ingredient;
        this.base = base;
        this.result = result;
    }

    public String getPermission() {
        return permission;
    }

    public void setPermission(String permission) {
        this.permission = permission;
    }

    public BrewingRecipeItem getIngredient() {
        return ingredient;
    }

    public void setIngredient(BrewingRecipeItem ingredient) {
        this.ingredient = ingredient;
    }

    public BrewingRecipeItem getBase() {
        return base;
    }

    public void setBase(BrewingRecipeItem base) {
        this.base = base;
    }

    public BrewingRecipeItem getResult() {
        return result;
    }

    public void setResult(BrewingRecipeItem result) {
        this.result = result;
    }


}

package com.wavesfx.wavesfx.gui.transfer;

import com.wavesfx.wavesfx.logic.Transferable;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ObjectProperty;

public class AssetChoice {
    private final ObjectProperty<Transferable> transferable;
    private final BooleanProperty isSelected;

    public AssetChoice(ObjectProperty<Transferable> transferable, BooleanProperty booleanProperty) {
        this.transferable = transferable;
        this.isSelected = booleanProperty;
    }

    public ObjectProperty<Transferable> transferableProperty() {
        return transferable;
    }

    public void setTransferable(Transferable transferable) {
        this.transferable.set(transferable);
    }

    public Transferable getTransferable() {
        return transferable.get();
    }

    public boolean isIsSelected() {
        return isSelected.get();
    }

    public BooleanProperty isSelectedProperty() {
        return isSelected;
    }

    public void setIsSelected(boolean isSelected) {
        this.isSelected.set(isSelected);
    }
}

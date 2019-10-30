open module wavesfx {
    requires transitive javafx.fxml;
    requires transitive javafx.graphics;
    requires transitive javafx.controls;
    requires java.desktop;
    requires core;
    requires com.fasterxml.jackson.annotation;
    requires com.fasterxml.jackson.core;
    requires com.fasterxml.jackson.databind;
    requires rxjavafx;
    requires io.reactivex.rxjava2;
    requires org.kordamp.iconli.core;
    requires org.kordamp.ikonli.javafx;
    requires org.kordamp.ikonli.materialdesign;
    requires WavesJ.master.SNAPSHOT;
    requires org.apache.logging.log4j;
}
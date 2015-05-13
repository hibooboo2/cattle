package io.cattle.platform.allocator.constraint;

public class AffinityConstraintDefinition {
    public enum AffinityOps {
        SOFT_EQ("==~", "{eq~}"),
        SOFT_NE("!=~", "{ne~}"),
        EQ("==", "{eq}"),
        NE("!=", "{ne}");

        String envSymbol;
        String labelSymbol;

        private AffinityOps(String envSymbol, String labelSymbol) {
            this.envSymbol = envSymbol;
            this.labelSymbol = labelSymbol;
        }
    }

    AffinityOps op;
    String key;
    String value;

    public AffinityConstraintDefinition(AffinityOps op, String key, String value) {
        this.op = op;
        this.key = key;
        this.value = value;
    }
}

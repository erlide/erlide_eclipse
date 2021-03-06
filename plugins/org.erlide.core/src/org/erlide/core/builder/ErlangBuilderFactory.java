package org.erlide.core.builder;

import org.erlide.core.builder.external.EmakeBuilder;
import org.erlide.core.builder.external.MakeBuilder;
import org.erlide.core.builder.external.RebarBuilder;
import org.erlide.engine.model.builder.BuilderTool;

public class ErlangBuilderFactory {

    public static ErlangBuilder get(final BuilderTool tool) {
        switch (tool) {
        case INTERNAL:
            return new InternalBuilder();
        case REBAR:
            return new RebarBuilder();
        case EMAKE:
            return new EmakeBuilder();
        case MAKE:
            return new MakeBuilder();
        default:
            break;
        }
        return null;
    }

    private ErlangBuilderFactory() {
    }
}

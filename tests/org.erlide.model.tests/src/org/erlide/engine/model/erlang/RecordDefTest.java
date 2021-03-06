package org.erlide.engine.model.erlang;

import static com.google.common.truth.Truth.assertThat;

import java.util.List;

import org.erlide.engine.model.ErlElementKind;
import org.erlide.engine.model.IErlElement;
import org.erlide.engine.model.root.IErlModule;
import org.erlide.engine.util.ErlideTestUtils;
import org.junit.Test;

public class RecordDefTest extends ErlModelTestBase {

    @Test
    public void detectRecordName() throws Exception {
        final IErlModule module2 = ErlideTestUtils.createModule(project, "yy.erl",
                "-module(yy).\n" + "-record(X, {x}).\n" + "-record(X , {x}).\n"
                        + "f()->?X.\n");
        module2.open(null);
        final List<IErlElement> childrenOfKind = module2
                .getChildrenOfKind(ErlElementKind.RECORD_DEF);

        IErlRecordDef def = (IErlRecordDef) childrenOfKind.get(0);
        assertThat(def.getDefinedName()).isEqualTo("X");

        def = (IErlRecordDef) childrenOfKind.get(1);
        assertThat(def.getDefinedName()).isEqualTo("X");
    }

}

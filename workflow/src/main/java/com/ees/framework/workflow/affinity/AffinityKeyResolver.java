package com.ees.framework.workflow.affinity;

import com.ees.framework.context.FxAffinity;
import com.ees.framework.context.FxContext;

public interface AffinityKeyResolver {

    FxAffinity resolve(FxContext<?> context);
}

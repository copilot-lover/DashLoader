package dev.notalpha.dashloader.minecraft.model.components;

import dev.notalpha.dashloader.cache.registry.RegistryReader;
import dev.notalpha.dashloader.cache.registry.RegistryWriter;
import dev.notalpha.dashloader.mixin.accessor.ModelOverrideListBakedOverrideAccessor;
import dev.quantumfusion.hyphen.scan.annotations.DataNullable;
import net.minecraft.client.render.model.BakedModel;
import net.minecraft.client.render.model.json.ModelOverrideList;
import org.jetbrains.annotations.Nullable;

public final class DashModelOverrideListBakedOverride {
	public final DashModelOverrideListInlinedCondition[] conditions;
	@DataNullable
	public final Integer model;

	public DashModelOverrideListBakedOverride(DashModelOverrideListInlinedCondition[] conditions, @Nullable Integer model) {
		this.conditions = conditions;
		this.model = model;
	}

	public DashModelOverrideListBakedOverride(ModelOverrideList.BakedOverride override, RegistryWriter writer) {
		final ModelOverrideList.InlinedCondition[] conditionsIn = ((ModelOverrideListBakedOverrideAccessor) override).getConditions();
		BakedModel bakedModel = ((ModelOverrideListBakedOverrideAccessor) override).getModel();
		this.model = bakedModel == null ? null : writer.add(bakedModel);

		this.conditions = new DashModelOverrideListInlinedCondition[conditionsIn.length];
		for (int i = 0; i < conditionsIn.length; i++) {
			this.conditions[i] = new DashModelOverrideListInlinedCondition(conditionsIn[i]);
		}
	}

	public ModelOverrideList.BakedOverride export(RegistryReader reader) {
		var conditionsOut = new ModelOverrideList.InlinedCondition[this.conditions.length];
		for (int i = 0; i < this.conditions.length; i++) {
			conditionsOut[i] = this.conditions[i].export();
		}

		return ModelOverrideListBakedOverrideAccessor.newModelOverrideListBakedOverride(conditionsOut, this.model == null ? null : reader.get(this.model));
	}
}
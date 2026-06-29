const metrics = [
    { label: '报修总量', value: '0', note: '待接入 /api/dashboard/summary' },
    { label: '处理中工单', value: '0', note: '按组织数据范围统计' },
    { label: '培训完成率', value: '0%', note: '基于已发布任务计算' },
    { label: '培训通过率', value: '0%', note: '默认 60 分及格' },
];
const __VLS_ctx = {
    ...{},
    ...{},
};
let __VLS_components;
let __VLS_intrinsics;
let __VLS_directives;
__VLS_asFunctionalElement1(__VLS_intrinsics.section, __VLS_intrinsics.section)({
    ...{ class: "page-grid" },
});
/** @type {__VLS_StyleScopedClasses['page-grid']} */ ;
for (const [item] of __VLS_vFor((__VLS_ctx.metrics))) {
    let __VLS_0;
    /** @ts-ignore @type { | typeof __VLS_components.elCard | typeof __VLS_components.ElCard | typeof __VLS_components['el-card'] | typeof __VLS_components.elCard | typeof __VLS_components.ElCard | typeof __VLS_components['el-card']} */
    elCard;
    // @ts-ignore
    const __VLS_1 = __VLS_asFunctionalComponent1(__VLS_0, new __VLS_0({
        key: (item.label),
        shadow: "never",
    }));
    const __VLS_2 = __VLS_1({
        key: (item.label),
        shadow: "never",
    }, ...__VLS_functionalComponentArgsRest(__VLS_1));
    const { default: __VLS_5 } = __VLS_3.slots;
    __VLS_asFunctionalElement1(__VLS_intrinsics.span, __VLS_intrinsics.span)({
        ...{ class: "metric-label" },
    });
    /** @type {__VLS_StyleScopedClasses['metric-label']} */ ;
    (item.label);
    __VLS_asFunctionalElement1(__VLS_intrinsics.strong, __VLS_intrinsics.strong)({
        ...{ class: "metric-value" },
    });
    /** @type {__VLS_StyleScopedClasses['metric-value']} */ ;
    (item.value);
    __VLS_asFunctionalElement1(__VLS_intrinsics.p, __VLS_intrinsics.p)({});
    (item.note);
    // @ts-ignore
    [metrics,];
    var __VLS_3;
    // @ts-ignore
    [];
}
// @ts-ignore
[];
const __VLS_export = (await import('vue')).defineComponent({});
export default {};

/* 应用启动 */
(function (Gym) {
    const app = Vue.createApp(Gym.rootOptions);
    app.use(ElementPlus, { locale: window.ElementPlusLocaleZhCn });
    // 全局混入：为所有页面提供标签映射，模板中直接用标识符访问
    app.mixin({
        computed: {
            resTag: () => Gym.resTag,
            scheduleTag: () => Gym.scheduleTag,
            equipTag: () => Gym.equipTag,
            diffTag: () => Gym.diffTag,
            categoryText: () => Gym.categoryText,
            diffText: () => Gym.diffText,
            courseTypeMap: () => Gym.courseType
        }
    });
    Object.keys(Gym.pages).forEach(name => app.component(name, Gym.pages[name]));
    app.mount('#app');
})(window.Gym);

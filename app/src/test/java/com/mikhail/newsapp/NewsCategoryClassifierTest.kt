package com.mikhail.newsapp

import org.junit.Assert.assertEquals
import org.junit.Test

/** Unit-тесты локального классификатора категорий новостей. */
class NewsCategoryClassifierTest {

    @Test
    fun `классифицирует несколько категорий по заголовку`() {
        assertEquals(NewsCategory.TECH, NewsCategoryClassifier.classify(
            "Вышел новый смартфон с процессором Snapdragon", null, null))
        assertEquals(NewsCategory.SCIENCE, NewsCategoryClassifier.classify(
            "Ученые открыли новую планету в космосе", null, null))
        assertEquals(NewsCategory.SPORT, NewsCategoryClassifier.classify(
            "Сборная выиграла чемпионат по футболу", null, null))
        assertEquals(NewsCategory.POLITICS, NewsCategoryClassifier.classify(
            "Президент подписал новый законопроект", null, null))
        assertEquals(NewsCategory.ECONOMY, NewsCategoryClassifier.classify(
            "Курс доллара вырос на фондовом рынке", null, null))
        assertEquals(NewsCategory.GAMES, NewsCategoryClassifier.classify(
            "Новый патч для компьютерной игры вышел сегодня", null, null))
        assertEquals(NewsCategory.AUTO, NewsCategoryClassifier.classify(
            "Тесла выпустила новую электрическую машину", null, null))
        assertEquals(NewsCategory.ENTERTAINMENT, NewsCategoryClassifier.classify(
            "Новый фильм с 유명ным актером в главной роли", null, null))
    }

    @Test
    fun `игнорирует регистр и лишние пробелы`() {
        assertEquals(NewsCategory.TECH, NewsCategoryClassifier.classify(
            "  ТЕХНОЛОГИИ  и 程序мирование ", null, null))
        assertEquals(NewsCategory.SCIENCE, NewsCategoryClassifier.classify(
            "НаУКа и ИсСледоВАНие", null, null))
        assertEquals(NewsCategory.SPORT, NewsCategoryClassifier.classify(
            " Футбол   БАСКЕТБОЛЛ хоккей ", null, null))
    }

    @Test
    fun `пустой заголовок и описание возвращают Другое`() {
        assertEquals(NewsCategory.OTHER, NewsCategoryClassifier.classify("", null, null))
        assertEquals(NewsCategory.OTHER, NewsCategoryClassifier.classify(null, null, null))
        assertEquals(NewsCategory.OTHER, NewsCategoryClassifier.classify("", "", null))
        assertEquals(NewsCategory.OTHER, NewsCategoryClassifier.classify("   ", "   ", null))
    }

    @Test
    fun `отсутствие ключевых слов возвращает Другое`() {
        assertEquals(NewsCategory.OTHER, NewsCategoryClassifier.classify(
            "Сегодня хорошая погода для прогулки", null, null))
        assertEquals(NewsCategory.OTHER, NewsCategoryClassifier.classify(
            "Новости за неделю", "Просто текст без тематических слов", null))
    }

    @Test
    fun `конфликтующие признаки выигрывает категория с большим числом совпадений`() {
        // Технологии и политика оба упомянуты, но технологий больше
        assertEquals(NewsCategory.TECH, NewsCategoryClassifier.classify(
            "Новый законопроект о регулировании интернет-технологий и разработке софта",
            "Парламент обсуждает закон о технологиях и программировании", null))
    }

    @Test
    fun `ложные совпадения на частях слов не срабатывают`() {
        // "код" не должен срабатывать на "кодекс"
        assertEquals(NewsCategory.OTHER, NewsCategoryClassifier.classify(
            "Уголовный кодекс Российской Федерации", null, null))
        // "мир" не должен срабатывать на "власть" или "мирный"
        assertEquals(NewsCategory.POLITICS, NewsCategoryClassifier.classify(
            "Власть приняла решение о мире", null, null))
        // "авто" не должен срабатывать на "авторитет"
        assertNotTech(NewsCategoryClassifier.classify(
            "Авторитетный источник сообщил о происшествии", null, null))
    }

    @Test
    fun `приоритет явных признаков над названием источника`() {
        // Источник звучит технологично, но контент — о спорте
        assertEquals(NewsCategory.SPORT, NewsCategoryClassifier.classify(
            "Финал чемпионата по футболу прошел в стадионе",
            null, "ТехноНовости.ру"))
        // Контент о науке, источник sounds like auto
        assertEquals(NewsCategory.SCIENCE, NewsCategoryClassifier.classify(
            "Исследование квантовой физики в лаборатории",
            null, "АвтоМир журнал"))
    }

    @Test
    fun `стабильность результата при повторной классификации`() {
        val title = "Новый смартфон с камерой и процессором"
        val desc = "Тестирование гаджета показало отличные результаты"
        val first = NewsCategoryClassifier.classify(title, desc, null)
        val second = NewsCategoryClassifier.classify(title, desc, null)
        val third = NewsCategoryClassifier.classify(title, desc, null)
        assertEquals(first, second)
        assertEquals(second, third)
        assertEquals(NewsCategory.TECH, first)
    }

    @Test
    fun `описание тоже участвует в классификации`() {
        assertEquals(NewsCategory.TECH, NewsCategoryClassifier.classify(
            "Новости дня", "Релиз новой операционной системы для смартфонов", null))
        assertEquals(NewsCategory.SCIENCE, NewsCategoryClassifier.classify(
            "Сегодня в науке", "Ученые провели эксперимент с квантовыми частицами", null))
    }

    private fun assertNotTech(category: NewsCategory) {
        org.junit.Assert.assertNotEquals("Ожидалось не-Технологии, но получено Технологии",
            NewsCategory.TECH, category)
    }
}

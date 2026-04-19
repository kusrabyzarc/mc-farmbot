package com.kusrabyzarc.farmbot;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.CyclingButtonWidget;
import net.minecraft.client.gui.widget.SliderWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.screen.ScreenTexts;
import net.minecraft.text.Text;

public class FarmbotConfigScreen extends Screen {
	private static final Text TITLE = Text.translatable("config.farmbot.title");
	private static final Text CONTROLS_HINT = Text.translatable("config.farmbot.controls_hint");

	private final Screen parent;
	private final FarmbotConfig config;

	private CyclingButtonWidget<Boolean> escapeOnLowHealthButton;
	private HealthThresholdSliderWidget healthThresholdSlider;
	private CyclingButtonWidget<Boolean> disconnectWhenFoodRunsOutButton;
	private TextFieldWidget attackEpsilonField;
	private ButtonWidget doneButton;
	private boolean attackEpsilonValid;
	private boolean attackEpsilonZero;
	private float parsedAttackEpsilonTicks;

	public FarmbotConfigScreen(Screen parent) {
		super(TITLE);
		this.parent = parent;
		this.config = FarmbotConfig.get().copy();
	}

	@Override
	protected void init() {
		int contentWidth = 280;
		int left = this.width / 2 - contentWidth / 2;
		int y = 64;

		this.escapeOnLowHealthButton = this.addDrawableChild(
			CyclingButtonWidget.onOffBuilder()
				.initially(this.config.escapeOnLowHealth)
				.build(left, y, contentWidth, 20, Text.translatable("config.farmbot.escape_on_low_health"), (button, value) -> {
					this.config.escapeOnLowHealth = value;
					this.updateWidgetStates();
				})
		);
		y += 24;

		this.healthThresholdSlider = this.addDrawableChild(
			new HealthThresholdSliderWidget(left, y, contentWidth, 20, this.config)
		);
		y += 24;

		this.disconnectWhenFoodRunsOutButton = this.addDrawableChild(
			CyclingButtonWidget.onOffBuilder()
				.initially(this.config.disconnectWhenFoodRunsOut)
				.build(
					left,
					y,
					contentWidth,
					20,
					Text.translatable("config.farmbot.disconnect_when_food_runs_out"),
					(button, value) -> this.config.disconnectWhenFoodRunsOut = value
				)
		);
		y += 24;

		this.attackEpsilonField = this.addDrawableChild(
			new TextFieldWidget(this.textRenderer, left, y, contentWidth, 20, Text.translatable("config.farmbot.attack_epsilon_ticks_label"))
		);
		this.attackEpsilonField.setMaxLength(16);
		this.attackEpsilonField.setText(Float.toString(this.config.attackEpsilonTicks));
		this.attackEpsilonField.setChangedListener(this::onAttackEpsilonChanged);
		this.setInitialFocus(this.attackEpsilonField);
		this.onAttackEpsilonChanged(this.attackEpsilonField.getText());

		int buttonY = this.height - 28;
		int buttonWidth = 90;
		int buttonGap = 5;
		int buttonLeft = this.width / 2 - (buttonWidth * 3 + buttonGap * 2) / 2;

		this.addDrawableChild(
			ButtonWidget.builder(Text.translatable("config.farmbot.reset"), button -> this.resetToDefaults())
				.dimensions(buttonLeft, buttonY, buttonWidth, 20)
				.build()
		);
		this.addDrawableChild(
			ButtonWidget.builder(ScreenTexts.CANCEL, button -> this.close())
				.dimensions(buttonLeft + buttonWidth + buttonGap, buttonY, buttonWidth, 20)
				.build()
		);
		this.doneButton = this.addDrawableChild(
			ButtonWidget.builder(ScreenTexts.DONE, button -> this.saveAndClose())
				.dimensions(buttonLeft + (buttonWidth + buttonGap) * 2, buttonY, buttonWidth, 20)
				.build()
		);

		this.updateWidgetStates();
	}

	@Override
	public void render(DrawContext context, int mouseX, int mouseY, float deltaTicks) {
		this.renderDarkening(context);
		super.render(context, mouseX, mouseY, deltaTicks);

		context.drawCenteredTextWithShadow(this.textRenderer, this.title, this.width / 2, 20, 0xFFFFFF);
		context.drawWrappedTextWithShadow(this.textRenderer, CONTROLS_HINT, this.width / 2 - 140, 38, 280, 0xA0A0A0);
		context.drawWrappedTextWithShadow(
			this.textRenderer,
			Text.translatable("config.farmbot.attack_epsilon_recommendation"),
			this.width / 2 - 140,
			this.height - 72,
			280,
			0xA0A0A0
		);

		if (!this.attackEpsilonValid || this.attackEpsilonZero) {
			Text warningText = this.attackEpsilonZero
				? Text.translatable("config.farmbot.attack_epsilon_zero_warning")
				: Text.translatable("config.farmbot.attack_epsilon_negative_warning");
			int warningColor = this.attackEpsilonValid ? 0xE0C060 : 0xFF8080;
			context.drawWrappedTextWithShadow(this.textRenderer, warningText, this.width / 2 - 140, this.height - 56, 280, warningColor);
		}
	}

	@Override
	public void close() {
		if (this.client != null) {
			this.client.setScreen(this.parent);
		}
	}

	private void saveAndClose() {
		if (!this.attackEpsilonValid) {
			return;
		}

		this.config.attackEpsilonTicks = this.parsedAttackEpsilonTicks;
		FarmbotConfig.get().copyFrom(this.config);
		FarmbotConfig.save();
		this.close();
	}

	private void resetToDefaults() {
		this.config.copyFrom(FarmbotConfig.defaults());
		this.escapeOnLowHealthButton.setValue(this.config.escapeOnLowHealth);
		this.healthThresholdSlider.setHealthPoints(this.config.criticalHealthThreshold);
		this.disconnectWhenFoodRunsOutButton.setValue(this.config.disconnectWhenFoodRunsOut);
		this.attackEpsilonField.setText(Float.toString(this.config.attackEpsilonTicks));
		this.onAttackEpsilonChanged(this.attackEpsilonField.getText());
		this.updateWidgetStates();
	}

	private void updateWidgetStates() {
		this.healthThresholdSlider.active = this.config.escapeOnLowHealth;
		if (this.doneButton != null) {
			this.doneButton.active = this.attackEpsilonValid;
		}
	}

	private void onAttackEpsilonChanged(String value) {
		try {
			float parsed = Float.parseFloat(value.trim());
			this.parsedAttackEpsilonTicks = parsed;
			this.attackEpsilonValid = parsed >= FarmbotConfig.MIN_ATTACK_EPSILON_TICKS;
			this.attackEpsilonZero = this.attackEpsilonValid && parsed == 0.0F;
		} catch (NumberFormatException exception) {
			this.parsedAttackEpsilonTicks = 0.0F;
			this.attackEpsilonValid = false;
			this.attackEpsilonZero = false;
		}

		this.updateWidgetStates();
	}

	private static final class HealthThresholdSliderWidget extends SliderWidget {
		private final FarmbotConfig config;

		private HealthThresholdSliderWidget(int x, int y, int width, int height, FarmbotConfig config) {
			super(x, y, width, height, ScreenTexts.EMPTY, toSliderValue(config.criticalHealthThreshold));
			this.config = config;
			this.updateMessage();
		}

		@Override
		protected void updateMessage() {
			double hearts = this.config.criticalHealthThreshold / 2.0F;
			this.setMessage(
				Text.translatable("config.farmbot.critical_health_threshold", formatHearts(hearts))
			);
		}

		@Override
		protected void applyValue() {
			this.config.criticalHealthThreshold = toHealthPoints(this.value);
			this.updateMessage();
		}

		private void setHealthPoints(float healthPoints) {
			this.config.criticalHealthThreshold = healthPoints;
			this.value = toSliderValue(healthPoints);
			this.updateMessage();
		}

		private static double toSliderValue(float healthPoints) {
			return (healthPoints - FarmbotConfig.MIN_CRITICAL_HEALTH_THRESHOLD)
				/ (FarmbotConfig.MAX_CRITICAL_HEALTH_THRESHOLD - FarmbotConfig.MIN_CRITICAL_HEALTH_THRESHOLD);
		}

		private static float toHealthPoints(double sliderValue) {
			double rawHearts = 1.0 + sliderValue * 9.0;
			double snappedHearts = Math.round(rawHearts * 2.0) / 2.0;
			return (float) (snappedHearts * 2.0);
		}

		private static String formatHearts(double hearts) {
			if (Math.rint(hearts) == hearts) {
				return Integer.toString((int) hearts);
			}

			return Double.toString(hearts);
		}
	}
}

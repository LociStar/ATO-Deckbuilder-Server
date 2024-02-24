package com.loci.ato_deck_builder_server.database.objects;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

@Data
@ToString
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Table("card_details")
public class CardDetails {
    @Column("ac_energy_bonus")
    @JsonProperty("AcEnergyBonus")
    private String acEnergyBonus;

    @Column("ac_energy_bonus2")
    private String acEnergyBonus2;

    @Column("ac_energy_bonus_quantity")
    private int acEnergyBonusQuantity;

    @Column("ac_energy_bonus2_quantity")
    private int acEnergyBonus2Quantity;

    @Column("add_card")
    private int addCard;

    @Column("add_card_choose")
    private int addCardChoose;

    @Column("add_card_cost_turn")
    private boolean addCardCostTurn;

    @Column("add_card_from")
    private String addCardFrom;

    @Column("add_card_id")
    private String addCardId;

    @Column("add_card_list")
    private String[] addCardList;

    @Column("add_card_place")
    private String addCardPlace;

    @Column("add_card_reduced_cost")
    private int addCardReducedCost;

    @Column("add_card_type")
    private String addCardType;

    @Column("add_card_type_aux")
    private String[] addCardTypeAux;

    @Column("add_card_vanish")
    private boolean addCardVanish;

    @Column("aura")
    private String aura;

    @Column("aura2")
    private String aura2;

    @Column("aura3")
    private String aura3;

    @Column("aura_charges")
    private int auraCharges;

    @Column("aura_charges_special_value1")
    private boolean auraChargesSpecialValue1;

    @Column("aura_charges_special_value2")
    private boolean auraChargesSpecialValue2;

    @Column("aura_charges_special_value_global")
    private boolean auraChargesSpecialValueGlobal;

    @Column("aura_charges2")
    private int auraCharges2;

    @Column("aura_charges2_special_value1")
    private boolean auraCharges2SpecialValue1;

    @Column("aura_charges2_special_value2")
    private boolean auraCharges2SpecialValue2;

    @Column("aura_charges2_special_value_global")
    private boolean auraCharges2SpecialValueGlobal;

    @Column("aura_charges3")
    private int auraCharges3;

    @Column("aura_charges3_special_value1")
    private boolean auraCharges3SpecialValue1;

    @Column("aura_charges3_special_value2")
    private boolean auraCharges3SpecialValue2;

    @Column("aura_charges3_special_value_global")
    private boolean auraCharges3SpecialValueGlobal;

    @Column("aura_self")
    private String auraSelf;

    @Column("aura_self2")
    private String auraSelf2;

    @Column("aura_self3")
    private String auraSelf3;

    @Column("autoplay_draw")
    private boolean autoplayDraw;

    @Column("autoplay_end_turn")
    private boolean autoplayEndTurn;

    @Column("base_card")
    private String baseCard;

    @Column("card_class")
    private String cardClass;

    @Column("card_name")
    private String cardName;

    @Column("card_number")
    private int cardNumber;

    @Column("card_rarity")
    private String cardRarity;

    @Column("card_type")
    private String cardType;

    @Column("card_type_aux")
    private String cardTypeAux;

    @Column("card_type_list")
    private String[] cardTypeList;

    @Column("card_upgraded")
    private String cardUpgraded;

    @Column("corrupted")
    private boolean corrupted;

    @Column("curse")
    private String curse;

    @Column("curse2")
    private String curse2;

    @Column("curse3")
    private String curse3;

    @Column("curse_charges")
    private int curseCharges;

    @Column("curse_charges_special_value1")
    private boolean curseChargesSpecialValue1;

    @Column("curse_charges_special_value2")
    private boolean curseChargesSpecialValue2;

    @Column("curse_charges_special_value_global")
    private boolean curseChargesSpecialValueGlobal;

    @Column("curse_charges2")
    private int curseCharges2;

    @Column("curse_charges2_special_value1")
    private boolean curseCharges2SpecialValue1;

    @Column("curse_charges2_special_value2")
    private boolean curseCharges2SpecialValue2;

    @Column("curse_charges2_special_value_global")
    private boolean curseCharges2SpecialValueGlobal;

    @Column("curse_charges3")
    private int curseCharges3;

    @Column("curse_charges3_special_value1")
    private boolean curseCharges3SpecialValue1;

    @Column("curse_charges3_special_value2")
    private boolean curseCharges3SpecialValue2;

    @Column("curse_charges3_special_value_global")
    private boolean curseCharges3SpecialValueGlobal;

    @Column("curse_self")
    private String curseSelf;

    @Column("curse_self2")
    private String curseSelf2;

    @Column("curse_self3")
    private String curseSelf3;

    @Column("damage")
    private int damage;

    @Column("damage_special_value1")
    private boolean damageSpecialValue1;

    @Column("damage_special_value2")
    private boolean damageSpecialValue2;

    @Column("damage_special_value_global")
    private boolean damageSpecialValueGlobal;

    @Column("damage2")
    private int damage2;

    @Column("damage2_special_value1")
    private boolean damage2SpecialValue1;

    @Column("damage2_special_value2")
    private boolean damage2SpecialValue2;

    @Column("damage2_special_value_global")
    private boolean damage2SpecialValueGlobal;

    @Column("damage_energy_bonus")
    private int damageEnergyBonus;

    @Column("damage_self")
    private int damageSelf;

    @Column("damage_self2")
    private int damageSelf2;

    @Column("damage_sides")
    private int damageSides;

    @Column("damage_sides2")
    private int damageSides2;

    @Column("damage_type")
    private String damageType;

    @Column("damage_type2")
    private String damageType2;

    @Column("description")
    private String description;

    @Column("description_id")
    private String descriptionID;

    @Column("meds_custom_description")
    private String medsCustomDescription;

    @Column("discard_card")
    private int discardCard;

    @Column("discard_card_automatic")
    private boolean discardCardAutomatic;

    @Column("discard_card_place")
    private String discardCardPlace;

    @Column("discard_card_type")
    private String discardCardType;

    @Column("discard_card_type_aux")
    private String[] discardCardTypeAux;

    @Column("dispel_auras")
    private int dispelAuras;

    @Column("draw_card")
    private int drawCard;

    @Column("draw_card_special_value_global")
    private boolean drawCardSpecialValueGlobal;

    @Column("effect_cast_center")
    private boolean effectCastCenter;

    @Column("effect_caster")
    private String effectCaster;

    @Column("effect_caster_repeat")
    private boolean effectCasterRepeat;

    @Column("effect_post_cast_delay")
    private double effectPostCastDelay;

    @Column("effect_post_target_delay")
    private int effectPostTargetDelay;

    @Column("effect_pre_action")
    private String effectPreAction;

    @Column("effect_repeat")
    private int effectRepeat;

    @Column("effect_repeat_delay")
    private int effectRepeatDelay;

    @Column("effect_repeat_energy_bonus")
    private int effectRepeatEnergyBonus;

    @Column("effect_repeat_max_bonus")
    private int effectRepeatMaxBonus;

    @Column("effect_repeat_modificator")
    private int effectRepeatModificator;

    @Column("effect_repeat_target")
    private String effectRepeatTarget;

    @Column("effect_required")
    private String effectRequired;

    @Column("effect_target")
    private String effectTarget;

    @Column("effect_trail")
    private String effectTrail;

    @Column("effect_trail_angle")
    private String effectTrailAngle;

    @Column("effect_trail_repeat")
    private boolean effectTrailRepeat;

    @Column("effect_trail_speed")
    private double effectTrailSpeed;

    @Column("end_turn")
    private boolean endTurn;

    @Column("energy_cost")
    private int energyCost;

    @Column("energy_cost_for_show")
    private int energyCostForShow;

    @Column("energy_recharge")
    private int energyRecharge;

    @Column("energy_recharge_special_value_global")
    private boolean energyRechargeSpecialValueGlobal;

    @Column("energy_reduction_permanent")
    private int energyReductionPermanent;

    @Column("energy_reduction_temporal")
    private int energyReductionTemporal;

    @Column("energy_reduction_to_zero_permanent")
    private boolean energyReductionToZeroPermanent;

    @Column("energy_reduction_to_zero_temporal")
    private boolean energyReductionToZeroTemporal;

    @Column("exhaust_counter")
    private int exhaustCounter;

    @Column("flip_sprite")
    private boolean flipSprite;

    @Column("fluff")
    private String fluff;

    @Column("fluff_percent")
    private double fluffPercent;

    @Column("gold_gain_quantity")
    private int goldGainQuantity;

    @Column("heal")
    private int heal;

    @Column("heal_aura_curse_name")
    private String healAuraCurseName;

    @Column("heal_aura_curse_name2")
    private String healAuraCurseName2;

    @Column("heal_aura_curse_name3")
    private String healAuraCurseName3;

    @Column("heal_aura_curse_name4")
    private String healAuraCurseName4;

    @Column("heal_aura_curse_self")
    private String healAuraCurseSelf;

    @Column("heal_curses")
    private int healCurses;

    @Column("heal_energy_bonus")
    private int healEnergyBonus;

    @Column("heal_self")
    private int healSelf;

    @Column("heal_self_per_damage_done_percent")
    private double healSelfPerDamageDonePercent;

    @Column("heal_self_special_value1")
    private boolean healSelfSpecialValue1;

    @Column("heal_self_special_value2")
    private boolean healSelfSpecialValue2;

    @Column("heal_self_special_value_global")
    private boolean healSelfSpecialValueGlobal;

    @Column("heal_sides")
    private int healSides;

    @Column("heal_special_value1")
    private boolean healSpecialValue1;

    @Column("heal_special_value2")
    private boolean healSpecialValue2;

    @Column("heal_special_value_global")
    private boolean healSpecialValueGlobal;

    @Id
    @Column("id")
    private String id;

    @Column("ignore_block")
    private boolean ignoreBlock;

    @Column("ignore_block2")
    private boolean ignoreBlock2;

    @Column("increase_auras")
    private int increaseAuras;

    @Column("increase_curses")
    private int increaseCurses;

    @Column("innate")
    private boolean innate;

    @Column("is_pet_attack")
    private boolean IsPetAttack;

    @Column("is_pet_cast")
    private boolean IsPetCast;

    @Column("item")
    private String item;

    @Column("item_enchantment")
    private String itemEnchantment;

    @Column("kill_pet")
    private boolean killPet;

    @Column("lazy")
    private boolean lazy;

    @Column("look_cards")
    private int lookCards;

    @Column("look_cards_discard_up_to")
    private int lookCardsDiscardUpTo;

    @Column("look_cards_vanish_up_to")
    private int lookCardsVanishUpTo;

    @Column("max_in_deck")
    private int MaxInDeck;

    @Column("modified_by_trait")
    private boolean modifiedByTrait;

    @Column("move_to_center")
    private boolean moveToCenter;

    @Column("only_in_weekly")
    private boolean onlyInWeekly;

    @Column("pet_front")
    private boolean petFront;

    @Column("pet_invert")
    private boolean petInvert;

    @Column("pet_model")
    private String petModel;

    @Column("pet_offset")
    private String petOffset;

    @Column("pet_size")
    private String petSize;

    @Column("playable")
    private boolean playable;

    @Column("pull_target")
    private int pullTarget;

    @Column("push_target")
    private int pushTarget;

    @Column("reduce_auras")
    private int reduceAuras;

    @Column("reduce_curses")
    private int reduceCurses;

    @Column("related_card")
    private String relatedCard;

    @Column("related_card2")
    private String relatedCard2;

    @Column("related_card3")
    private String relatedCard3;

    @Column("self_health_loss")
    private int selfHealthLoss;

    @Column("self_health_loss_special_global")
    private boolean selfHealthLossSpecialGlobal;

    @Column("self_health_loss_special_value1")
    private boolean selfHealthLossSpecialValue1;

    @Column("self_health_loss_special_value2")
    private boolean selfHealthLossSpecialValue2;

    @Column("self_kill_hidden_seconds")
    private double selfKillHiddenSeconds;

    @Column("shards_gain_quantity")
    private int shardsGainQuantity;

    @Column("show_in_tome")
    private boolean showInTome;

    @Column("sku")
    private String sku;

    @Column("sound")
    private String sound;

    @Column("sound_pre_action")
    private String soundPreAction;

    @Column("sound_pre_action_female")
    private String soundPreActionFemale;

    @Column("special_aura_curse_name1")
    private String specialAuraCurseName1;

    @Column("special_aura_curse_name2")
    private String specialAuraCurseName2;

    @Column("special_aura_curse_name_global")
    private String specialAuraCurseNameGlobal;

    @Column("special_value1")
    private String specialValue1;

    @Column("special_value2")
    private String specialValue2;

    @Column("special_value_global")
    private String specialValueGlobal;

    @Column("special_value_modifier1")
    private double specialValueModifier1;

    @Column("special_value_modifier2")
    private double specialValueModifier2;

    @Column("special_value_modifier_global")
    private double specialValueModifierGlobal;

    @Column("sprite")
    private String sprite;

    @Column("starter")
    private boolean starter;

    @Column("steal_auras")
    private int stealAuras;

    @Column("summon_aura")
    private String summonAura;

    @Column("summon_aura2")
    private String summonAura2;

    @Column("summon_aura3")
    private String summonAura3;

    @Column("summon_aura_charges")
    private int summonAuraCharges;

    @Column("summon_aura_charges2")
    private int summonAuraCharges2;

    @Column("summon_aura_charges3")
    private int summonAuraCharges3;

    @Column("summon_unit")
    private String summonUnit;

    @Column("summon_unit_num")
    private int summonUnitNum;

    @Column("target_position")
    private String targetPosition;

    @Column("target_side")
    private String targetSide;

    @Column("target_type")
    private String targetType;

    @Column("transfer_curses")
    private int transferCurses;

    @Column("upgraded_from")
    private String upgradedFrom;

    @Column("upgrades_to1")
    private String upgradesTo1;

    @Column("upgrades_to2")
    private String upgradesTo2;

    @Column("upgrades_to_rare")
    private String upgradesToRare;

    @Column("vanish")
    private boolean vanish;

    @Column("visible")
    private boolean visible;

}

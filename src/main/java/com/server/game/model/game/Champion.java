package com.server.game.model.game;


import com.server.game.model.game.component.Entity;
import com.server.game.model.game.component.HealthComponent;
import com.server.game.model.game.component.skill.SkillComponent;
import com.server.game.model.game.component.AttributeComponent;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Delegate;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;


@Data
@EqualsAndHashCode(callSuper=false)
@AllArgsConstructor
@NoArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class Champion extends Entity {

    Short id;
    String name;
    String role;
    @Delegate
    AttributeComponent attributeComponent;
    @Delegate
    HealthComponent healthComponent;
    @Delegate
    SkillComponent skillComponent;

    
    
}

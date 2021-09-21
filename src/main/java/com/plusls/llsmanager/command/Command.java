package com.plusls.llsmanager.command;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import com.velocitypowered.api.command.CommandSource;

import java.util.concurrent.CompletableFuture;

public interface Command extends SuggestionProvider<CommandSource>, com.mojang.brigadier.Command<CommandSource> {
    public LiteralArgumentBuilder<CommandSource> createSubCommand();

    public default CompletableFuture<Suggestions> getSuggestions(final CommandContext<CommandSource> context, final SuggestionsBuilder builder) throws CommandSyntaxException {
        return builder.buildFuture();
    }

}
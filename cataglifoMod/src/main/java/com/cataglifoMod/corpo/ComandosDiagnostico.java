package com.cataglifoMod.corpo;

import com.mojang.brigadier.arguments.FloatArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraftforge.event.RegisterCommandsEvent;

/** Testa o corpo e os sensores no servidor antes de conectar uma política Python. */
final class ComandosDiagnostico {
    private ComandosDiagnostico() {}

    static void registrar(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("cataglifo")
                // Usa a mesma permissão dos comandos de manipulação do mundo.
                .requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                .then(Commands.argument("corpo", EntityArgument.entity())
                    .then(Commands.literal("observar").executes(ctx -> executar(ctx, "observar")))
                    .then(Commands.literal("parar").executes(ctx -> executar(ctx, "parar")))
                    .then(Commands.literal("reset").executes(ctx -> executar(ctx, "reset")))
                    .then(Commands.literal("frente")
                        .then(Commands.argument("ticks", IntegerArgumentType.integer(1, 20))
                            .executes(ctx -> executar(ctx, "frente"))))
                    .then(Commands.literal("girar")
                        .then(Commands.argument("yaw", FloatArgumentType.floatArg(-45, 45))
                            .then(Commands.argument("pitch", FloatArgumentType.floatArg(-45, 45))
                                .executes(ctx -> executar(ctx, "girar")))))));
    }

    private static int executar(CommandContext<CommandSourceStack> ctx, String operacao)
            throws CommandSyntaxException {
        if (!(EntityArgument.getEntity(ctx, "corpo") instanceof CorpoEntity corpo)) {
            ctx.getSource().sendFailure(Component.literal("Selecione uma entidade cataglifo:corpo"));
            return 0;
        }
        try {
            switch (operacao) {
                case "observar" -> {
                    String json = corpo.montarObservacao().toString();
                    ctx.getSource().sendSuccess(() -> Component.literal(json), false);
                }
                case "parar" -> corpo.pararAcao();
                // Reset usa posição e orientação de quem executa o comando.
                case "reset" -> corpo.resetarCorpo(ctx.getSource().getPosition(),
                        ctx.getSource().getRotation().y);
                case "frente" -> corpo.definirAcao(1, 0, 0, 0, false,
                        IntegerArgumentType.getInteger(ctx, "ticks"));
                case "girar" -> corpo.definirAcao(0, 0,
                        FloatArgumentType.getFloat(ctx, "yaw"),
                        FloatArgumentType.getFloat(ctx, "pitch"), false, 1);
                default -> throw new IllegalArgumentException("Operação desconhecida");
            }
            return 1;
        } catch (IllegalArgumentException | IllegalStateException erro) {
            ctx.getSource().sendFailure(Component.literal(erro.getMessage()));
            return 0;
        }
    }
}

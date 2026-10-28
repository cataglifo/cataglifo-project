package com.cataglifoMod.corpo;

/** Regressões executáveis sem abrir Minecraft ou instalar framework de testes. */
public final class ControleAcaoTest {
    public static void main(String[] args) {
        ControleAcao c = new ControleAcao();
        c.orientar(170, 0);
        c.definir(2, -2, 30, 200, true, 2);
        verificar(c.frente() == 1 && c.lado() == -1, "limites de translação");
        verificar(c.yaw() == -160 && c.pitch() == 45, "orientação e limite de giro");
        verificar(c.pular() && c.ativa(), "primeiro tick ativo");
        c.avancar();
        verificar(!c.pular() && c.ativa(), "salto não se repete");
        c.avancar();
        verificar(!c.ativa() && c.frente() == 0 && c.lado() == 0, "timeout para o movimento");

        c.definir(1, 0, 0, 0, true, 5);
        for (float invalido : new float[]{Float.NaN, Float.POSITIVE_INFINITY, Float.NEGATIVE_INFINITY}) {
            rejeitar(() -> c.definir(invalido, 0, 0, 0, false, 5));
            rejeitar(() -> c.definir(0, invalido, 0, 0, false, 5));
            rejeitar(() -> c.definir(0, 0, invalido, 0, false, 5));
            rejeitar(() -> c.definir(0, 0, 0, invalido, false, 5));
        }
        rejeitar(() -> c.definir(0, 0, 0, 0, false, 0));
        rejeitar(() -> c.definir(0, 0, 0, 0, false, 21));
        verificar(c.frente() == 1 && c.pular(), "entrada inválida não altera ação válida");
        c.parar();
        c.avancar();
        verificar(!c.ativa() && !c.pular() && c.frente() == 0, "reset limpa ação e salto");
        c.orientar(-540, -200);
        verificar(c.yaw() == -180 && c.pitch() == -90, "normalização no reset");
        c.definir(1, 0, 0, 0, false, 1);
        c.avancar();
        verificar(!c.ativa(), "ação de um tick expira");
        System.out.println("ControleAcao: regressões passaram");
    }

    private static void verificar(boolean ok, String caso) {
        if (!ok) throw new AssertionError(caso);
    }

    private static void rejeitar(Runnable chamada) {
        try { chamada.run(); }
        catch (IllegalArgumentException esperado) { return; }
        throw new AssertionError("Entrada inválida foi aceita");
    }
}

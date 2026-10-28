package com.cataglifoMod.corpo;

/** Estado de comandos independente do Minecraft para validar timeout e entradas. */
final class ControleAcao {
    static final int DURACAO_PADRAO = 5;
    static final int DURACAO_MAXIMA = 20;
    private float frente, lado, yaw, pitch;
    private boolean pular;
    private int restante;

    void orientar(float yaw, float pitch) {
        validar(yaw, pitch);
        this.yaw = normalizar(yaw);
        this.pitch = limitar(pitch, -90, 90);
    }

    void definir(float frente, float lado, float giraYaw, float giraPitch,
                 boolean pular, int duracao) {
        // Validamos tudo antes de alterar o estado: NaN não é corrigido por clamp.
        validar(frente, lado, giraYaw, giraPitch);
        if (duracao < 1 || duracao > DURACAO_MAXIMA) {
            throw new IllegalArgumentException("Duração deve estar entre 1 e 20 ticks");
        }
        this.frente = limitar(frente, -1, 1);
        this.lado = limitar(lado, -1, 1);
        // Evita saltos extremos na orientação vindos de mensagens malformadas.
        yaw = normalizar(yaw + limitar(giraYaw, -45, 45));
        pitch = limitar(pitch + limitar(giraPitch, -45, 45), -90, 90);
        this.pular = pular;
        restante = duracao;
    }

    void avancar() {
        // O salto é um pulso; a translação dura exatamente os ticks contratados.
        pular = false;
        if (restante > 0 && --restante == 0) parar();
    }

    void parar() {
        frente = lado = 0;
        pular = false;
        restante = 0;
    }

    float frente() { return frente; }
    float lado() { return lado; }
    float yaw() { return yaw; }
    float pitch() { return pitch; }
    boolean pular() { return pular; }
    boolean ativa() { return restante > 0; }

    private static float limitar(float valor, float min, float max) {
        return Math.max(min, Math.min(max, valor));
    }

    private static float normalizar(float angulo) {
        float r = angulo % 360;
        return r >= 180 ? r - 360 : r < -180 ? r + 360 : r;
    }

    private static void validar(float... valores) {
        for (float valor : valores) {
            if (!Float.isFinite(valor)) throw new IllegalArgumentException("Ação exige números finitos");
        }
    }
}

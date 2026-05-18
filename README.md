# EstudioGuard: Sistema Multi-Agente de Filtrado de Chats

**Práctica de Sistemas Inteligentes**  
**Implementación con JADE (Java Agent DEvelopment Framework)**

---

## 📝 Descripción del Proyecto

**EstudioGuard** es un Sistema Multi-Agente (SMA) diseñado para mitigar las distracciones durante periodos de alta concentración. El sistema actúa como un interceptor inteligente de mensajes de chat (simulando aplicaciones como WhatsApp o Telegram), analizando el contenido y el remitente para decidir qué mensajes deben interrumpir al usuario y cuáles deben ser silenciados.

El sistema se basa en un **Motor de Reglas Experto** alimentado por una base de conocimiento externa en **XML**, complementado por un **Clasificador Naive Bayes** entrenado a partir de esas mismas reglas, permitiendo filtrar tanto mensajes conocidos como mensajes nunca vistos. El clasificador se perfecciona en tiempo real mediante un mecanismo de **aprendizaje activo** que solicita retroalimentación al usuario cuando el sistema no tiene suficiente confianza.

---

## 🏗️ Arquitectura del Sistema

El sistema sigue una arquitectura distribuida compuesta por 5 agentes principales coordinados bajo el estándar FIPA:

1.  **`ControllerAgent` (Orquestador):** El punto de entrada del sistema. Crea programáticamente a los demás agentes, gestiona el estado global (Modo Estudio ON/OFF) y asegura un apagado limpio (patrón Killer).
2.  **`FilterAgent` (Motor de IA):** El núcleo inteligente. Implementado mediante una **Máquina de Estados Finitos (FSMBehaviour)** de 6 estados, combina el motor de reglas y el clasificador Naive Bayes para tomar decisiones de filtrado. Durante el Modo Estudio almacena en buffer los mensajes descartados para mostrarlos al desactivar el modo.
3.  **`ChatSimulatorAgent` (Percepción):** Simula el entorno externo. Genera tráfico de mensajes periódicamente mediante la creación de **agentes efímeros** (`MessageFetcherAgent`), aislando la lógica de adquisición.
4.  **`UIAgent` (Interfaz):** Actúa como puente entre la plataforma JADE y la interfaz gráfica **Swing**, permitiendo al usuario visualizar mensajes filtrados, estadísticas en tiempo real y el banner de aprendizaje activo.
5.  **`StatsAgent` (Monitorización):** Agente pasivo que recibe eventos de filtrado durante el Modo Estudio, acumula estadísticas por mensaje y remitente, y las envía periódicamente a la interfaz.

### Diagrama de Flujo
`Simulador` → *(ACL INFORM)* → `Filtro (IA)` → *(ACL INFORM)* → `UI (Visualización)`  
`Filtro (IA)` → *(ACL INFORM)* → `Estadísticas` → *(ACL INFORM)* → `UI (Visualización)`  
`Filtro (IA)` → *(ACL REQUEST)* → `UI` *(banner de aprendizaje)*  
`UI` → *(ACL REQUEST)* → `Controlador` → *(ACL REQUEST)* → `Simulador / Filtro (Control)`

---

## 🧠 Decisiones de Diseño y Justificación IA

*   **Representación del Conocimiento (XML):** Se utiliza `rules.xml` para separar la lógica de negocio (reglas de importancia) del motor de inferencia. Esto permite que el sistema sea auditable y modificable por el usuario final sin tocar el código.
*   **Razonamiento (Puntuación Ponderada):** En lugar de simples reglas booleanas, se usa un sistema de pesos que permite resolver conflictos (ej: un mensaje de un grupo ruidoso pero con una palabra clave urgente). Los pesos negativos permiten penalizar explícitamente mensajes de distracción.
*   **Clasificador Naive Bayes:** Para generalizar más allá de las reglas explícitas, el sistema incluye un clasificador Naive Bayes entrenado automáticamente a partir de las keywords del XML. Actúa como desempate en casos borderline y aprende en tiempo real de la retroalimentación del usuario.
*   **Aprendizaje Activo:** Cuando el sistema no tiene confianza en un mensaje (fuera del Modo Estudio), muestra un banner con cuenta atrás de 8 segundos. La respuesta del usuario amplía el vocabulario del clasificador para sesiones futuras, sin interrumpir el estudio.
*   **FSMBehaviour:** El uso de una máquina de estados en el filtrado asegura una gestión eficiente de los hilos de ejecución y una lógica de estados (Espera → Análisis → Decisión → Aprendizaje) clara y profesional. Se usa `block()` en lugar de `blockingReceive()` para no bloquear el hilo del agente mientras se espera respuesta del usuario.
*   **Entorno Reproducible (Maven):** El proyecto usa Maven para gestionar la dependencia de JADE de forma local, asegurando que cualquier evaluador pueda ejecutar el código con un solo clic.

---

## 🚀 Guía de Ejecución

### Requisitos Previos
*   **Java JDK 11** o superior.
*   **Maven** instalado (opcional, IntelliJ lo gestiona solo).
*   Archivo `jade.jar` ubicado en la carpeta `/lib` del proyecto.

### Pasos en IntelliJ IDEA
1.  **Importar:** Abre el proyecto y asegúrate de que IntelliJ reconozca el archivo `pom.xml`.
2.  **Verificar recursos:** Asegúrate de que `rules.xml` está en `src/main/resources/`.
3.  **Configurar Run:**
    *   Main Class: `jade.Boot`
    *   Program Arguments: `-gui agController:es.uni.mas.agents.ControllerAgent`
4.  **Ejecutar:** Haz clic en Play. Se lanzará el GUI de JADE y la ventana de **EstudioGuard**.

### Uso
- Pulsa **"Activar MODO ESTUDIO"** para iniciar el filtrado. Solo llegarán los mensajes importantes.
- El banner amarillo aparece cuando el sistema necesita ayuda para aprender (solo con el modo desactivado).
- Pulsa **"Desactivar MODO ESTUDIO"** para ver los mensajes que fueron retenidos durante la sesión.

---

## 🤖 Declaración de Uso de IA

De acuerdo con los requisitos de la asignatura, se declara que se ha utilizado Inteligencia Artificial Generativa (Antigravity AI) en las siguientes fases del proyecto:

1.  **Concepción e Ideación:** La IA ayudó a pivotar desde ejemplos clásicos hacia la idea original del "Modo Estudio", validando la viabilidad de los requisitos técnicos frente a la propuesta.
2.  **Arquitectura y Estructura:** Se utilizó la IA para diseñar el flujo de mensajes FIPA y proponer patrones avanzados como el *Agente Controlador* y el *Agente Efímero*.
3.  **Resolución de Errores y Debugging:** La IA asistió en la depuración de las transiciones de la máquina de estados (FSM) y la configuración de las plantillas de mensajes (`MessageTemplate`) para evitar el robo de mensajes entre comportamientos.
4.  **Documentación Técnica:** Apoyo en la redacción de la justificación de decisiones de diseño y en la elaboración del presente archivo de documentación.

**Justificación:** El uso de la IA se ha centrado en acelerar la implementación de patrones estándar de JADE y en la estructuración profesional del proyecto, permitiendo que el desarrollo se centre en la lógica de inteligencia y en el cumplimiento estricto de los requisitos de la práctica.

---

**Autor:** Mª Laura Hernández Hernández, Miguel Hurtado Rojas, Víctor Pérez García, Jonás Rodríguez Unanyan, Adrián Zazo Rubio  
**Asignatura:** Sistemas Inteligentes  
**Universidad:** Universidad Politécnica de Madrid
# EstudioGuard: Sistema Multi-Agente de Filtrado de Chats

**Práctica de Sistemas Inteligentes**  
**Implementación con JADE (Java Agent DEvelopment Framework)**

---

## 📝 Descripción del Proyecto

**EstudioGuard** es un Sistema Multi-Agente (SMA) diseñado para mitigar las distracciones durante periodos de alta concentración. El sistema actúa como un interceptor inteligente de mensajes de chat (simulando aplicaciones como WhatsApp o Telegram), analizando el contenido y el remitente para decidir qué mensajes deben interrumpir al usuario y cuáles deben ser silenciados.

El sistema se basa en un **Motor de Reglas Experto** alimentado por una base de conocimiento externa en **XML**, permitiendo una clasificación inteligente, explicable y personalizable sin necesidad de modificar el código fuente.

---

## 🏗️ Arquitectura del Sistema

El sistema sigue una arquitectura distribuida compuesta por 4 agentes principales coordinados bajo el estándar FIPA:

1.  **`ControllerAgent` (Orquestador):** El punto de entrada del sistema. Crea programáticamente a los demás agentes, gestiona el estado global (Modo Estudio ON/OFF) y asegura un apagado limpio (patrón Killer).
2.  **`FilterAgent` (Motor de IA):** El núcleo inteligente. Implementado mediante una **Máquina de Estados Finitos (FSMBehaviour)**, procesa los mensajes mediante un filtro bloqueante y aplica el razonamiento de puntuación ponderada.
3.  **`ChatSimulatorAgent` (Percepción):** Simula el entorno externo. Genera tráfico de mensajes periódicamente mediante la creación de **agentes efímeros** (`MessageFetcherAgent`), aislando la lógica de adquisición.
4.  **`UIAgent` (Interfaz):** Actúa como puente entre la plataforma JADE y la interfaz gráfica **Swing**, permitiendo al usuario visualizar mensajes filtrados y controlar el sistema.

### Diagrama de Flujo
`Simulador` → *(ACL INFORM)* → `Filtro (IA)` → *(ACL INFORM)* → `UI (Visualización)`  
`UI` → *(ACL REQUEST)* → `Controlador` → *(ACL REQUEST)* → `Simulador (Control)`

---

## 🧠 Decisiones de Diseño y Justificación IA

Para este proyecto se han tomado decisiones técnicas orientadas a la robustez y la extensibilidad:

*   **Representación del Conocimiento (XML):** Se utiliza `rules.xml` para separar la lógica de negocio (reglas de importancia) del motor de inferencia. Esto permite que el sistema sea auditable y modificable por el usuario final.
*   **Razonamiento (Puntuación Ponderada):** En lugar de simples reglas booleanas, se usa un sistema de pesos que permite resolver conflictos (ej: un mensaje de un grupo ruidoso pero con una palabra clave urgente).
*   **FSMBehaviour:** El uso de una máquina de estados en el filtrado asegura una gestión eficiente de los hilos de ejecución y una lógica de estados (Espera -> Análisis -> Decisión) clara y profesional.
*   **Filtro Bloqueante:** Se implementa `blockingReceive(MessageTemplate)` para optimizar el consumo de recursos, manteniendo al agente en estado *Waiting* hasta la llegada de un mensaje válido.
*   **Entorno Reproducible (Maven):** El proyecto usa Maven para gestionar la dependencia de JADE de forma local, asegurando que cualquier evaluador pueda ejecutar el código con un solo clic.

---

## 🚀 Guía de Ejecución

### Requisitos Previos
*   **Java JDK 11** o superior.
*   **Maven** instalado (opcional, IntelliJ lo gestiona solo).
*   Archivo `jade.jar` ubicado en la carpeta `/lib` del proyecto.

### Pasos en IntelliJ IDEA
1.  **Importar:** Abre el proyecto y asegúrate de que IntelliJ reconozca el archivo `pom.xml`.
2.  **Configurar Run:**
    *   Main Class: `jade.Boot`
    *   Program Arguments: `-gui agController:es.uni.mas.agents.ControllerAgent`
3.  **Ejecutar:** Haz clic en Play. Se lanzará el GUI de JADE y la ventana de **EstudioGuard**.

---

## 🤖 Declaración de Uso de IA

De acuerdo con los requisitos de la asignatura, se declara que se ha utilizado Inteligencia Artificial Generativa (Antigravity AI) en las siguientes fases del proyecto:

1.  **Concepción e Ideación:** La IA ayudó a pivotar desde ejemplos clásicos hacia la idea original del "Modo Estudio", validando la viabilidad de los requisitos técnicos frente a la propuesta.
2.  **Arquitectura y Estructura:** Se utilizó la IA para diseñar el flujo de mensajes FIPA y proponer patrones avanzados como el *Agente Controlador* y el *Agente Efímero*.
3.  **Resolución de Errores y Debugging:** La IA asistió en la depuración de las transiciones de la máquina de estados (FSM) y la configuración de las plantillas de mensajes (`MessageTemplate`) para evitar el robo de mensajes entre comportamientos.
4.  **Documentación Técnica:** Apoyo en la redacción de la justificación de decisiones de diseño y en la elaboración del presente archivo de documentación.

**Justificación:** El uso de la IA se ha centrado en acelerar la implementación de patrones estándar de JADE y en la estructuración profesional del proyecto, permitiendo que el desarrollo se centre en la lógica de inteligencia y en el cumplimiento estricto de los requisitos de la práctica.

---

**Autor:** [Tu Nombre]  
**Asignatura:** Sistemas Inteligentes  
**Universidad:** [Tu Universidad]

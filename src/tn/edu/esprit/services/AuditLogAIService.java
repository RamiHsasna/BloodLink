package tn.edu.esprit.services;

import tn.edu.esprit.entities.BloodTransferRequestLog;
import tn.edu.esprit.entities.DonationLog;
import tn.edu.esprit.entities.DonorAlert;
import tn.edu.esprit.entities.DonorResponse;

import java.sql.Timestamp;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public class AuditLogAIService {

    private static final int SAMPLE_LIMIT = 12;
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private final MetierIAService metierIAService;

    public AuditLogAIService() {
        this.metierIAService = new MetierIAService();
    }

    public String analyzeAuditLogs(
            List<DonationLog> donationLogs,
            List<BloodTransferRequestLog> transferLogs,
            List<DonorAlert> donorAlerts,
            String filterContext) {
        List<DonationLog> safeDonationLogs = donationLogs != null ? donationLogs : List.of();
        List<BloodTransferRequestLog> safeTransferLogs = transferLogs != null ? transferLogs : List.of();
        List<DonorAlert> safeDonorAlerts = donorAlerts != null ? donorAlerts : List.of();

        if (safeDonationLogs.isEmpty() && safeTransferLogs.isEmpty() && safeDonorAlerts.isEmpty()) {
            return "Aucune donnée ne correspond aux filtres actuels. Ajustez la recherche ou la plage de dates puis relancez l'analyse.";
        }

        String localSummary = buildLocalSummary(safeDonationLogs, safeTransferLogs, safeDonorAlerts, filterContext);
        String prompt = buildPrompt(safeDonationLogs, safeTransferLogs, safeDonorAlerts, filterContext, localSummary);

        try {
            String response = metierIAService.generateAuditLogAnalysis(prompt);
            if (response == null || response.isBlank()) {
                return formatAnalysisForDisplay(localSummary);
            }
            return formatAnalysisForDisplay(response);
        } catch (Exception ex) {
            String message = ex.getMessage() != null && !ex.getMessage().isBlank()
                    ? ex.getMessage()
                    : "le fournisseur IA n'a renvoyé aucune réponse exploitable";
            return formatAnalysisForDisplay(localSummary)
                    + "\n\nAnalyse IA externe indisponible pour le moment. La synthèse locale reste affichée. Détail technique: "
                    + compactProviderMessage(message);
        }
    }

    private String compactProviderMessage(String message) {
        if (message == null || message.isBlank()) {
            return "provider unavailable";
        }
        String compact = message.replaceAll("(?i)Bearer\\s+[^\\s]+", "Bearer ***")
                .replaceAll("sk-[A-Za-z0-9_\\-]+", "***")
                .trim();
        if (compact.length() > 180) {
            return compact.substring(0, 177) + "...";
        }
        return compact;
    }

    private String buildPrompt(
            List<DonationLog> donationLogs,
            List<BloodTransferRequestLog> transferLogs,
            List<DonorAlert> donorAlerts,
            String filterContext,
            String localSummary) {
        return "Tu es un analyste opérationnel BloodLink.\n"
                + "Analyse les journaux d'audit et rédige une synthèse courte, claire et actionnable en français.\n"
                + "Contraintes:\n"
                + "- Réponse en 4 sections exactement: Résumé, Signaux faibles, Risques, Recommandations.\n"
                + "- Chaque section doit contenir 2 à 4 puces maximum.\n"
                + "- Pas de markdown complexe, pas de tableaux, pas de jargon inutile.\n"
                + "- Si un signal n'est pas démontré par les données, dis-le explicitement.\n"
                + "- Tient compte du contexte de filtre fourni.\n\n"
                + "Contexte des filtres:\n"
                + filterContext + "\n\n"
                + "Résumé local calculé par l'application:\n"
                + localSummary + "\n\n"
                + "Échantillon des journaux de dons:\n"
                + formatDonationSamples(donationLogs) + "\n\n"
                + "Échantillon des journaux de transferts:\n"
                + formatTransferSamples(transferLogs) + "\n\n"
                + "Échantillon des réponses donneurs:\n"
                + formatDonorAlertSamples(donorAlerts);
    }

    private String buildLocalSummary(
            List<DonationLog> donationLogs,
            List<BloodTransferRequestLog> transferLogs,
            List<DonorAlert> donorAlerts,
            String filterContext) {
        long collected = donationLogs.stream()
                .filter(log -> log.getAction() != null && "COLLECTED".equalsIgnoreCase(log.getAction().name()))
                .count();
        long rejected = donationLogs.stream()
                .filter(log -> log.getAction() != null && "REJECTED".equalsIgnoreCase(log.getAction().name()))
                .count();
        long approved = transferLogs.stream()
                .filter(log -> log.getAction() != null && "APPROVED".equalsIgnoreCase(log.getAction().name()))
                .count();
        long cancelled = transferLogs.stream()
                .filter(log -> log.getAction() != null && "CANCELLED".equalsIgnoreCase(log.getAction().name()))
                .count();
        long read = donorAlerts.stream().filter(DonorAlert::isRead).count();
        long interested = donorAlerts.stream()
                .filter(alert -> alert.getDonorResponse() == DonorResponse.INTERESTED)
                .count();

        StringBuilder builder = new StringBuilder();
        builder.append("Contexte: ").append(filterContext).append("\n");
        builder.append("- Journaux de dons: ").append(donationLogs.size())
                .append(" (collectes=").append(collected)
                .append(", rejets=").append(rejected).append(")\n");
        builder.append("- Journaux de transferts: ").append(transferLogs.size())
                .append(" (approuvés=").append(approved)
                .append(", annulés=").append(cancelled).append(")\n");
        builder.append("- Réponses donneurs: ").append(donorAlerts.size())
                .append(" (lus=").append(read)
                .append(", intéressés=").append(interested).append(")");
        return builder.toString();
    }

    private String formatDonationSamples(List<DonationLog> donationLogs) {
        if (donationLogs.isEmpty()) {
            return "- Aucun journal de don";
        }
        return donationLogs.stream()
                .sorted(Comparator.comparing(DonationLog::getCreatedAt, this::compareTimestamps).reversed())
                .limit(SAMPLE_LIMIT)
                .map(log -> "- " + safeTimestamp(log.getCreatedAt())
                        + " | don=" + safe(log.getDonationId())
                        + " | action=" + safe(log.getAction() != null ? log.getAction().name() : null)
                        + " | " + safe(log.getPreviousStatus()) + " -> " + safe(log.getNewStatus())
                        + " | notes=" + safe(log.getNotes()))
                .collect(Collectors.joining("\n"));
    }

    private String formatTransferSamples(List<BloodTransferRequestLog> transferLogs) {
        if (transferLogs.isEmpty()) {
            return "- Aucun journal de transfert";
        }
        return transferLogs.stream()
                .sorted(Comparator.comparing(BloodTransferRequestLog::getCreatedAt, this::compareTimestamps).reversed())
                .limit(SAMPLE_LIMIT)
                .map(log -> "- " + safeTimestamp(log.getCreatedAt())
                        + " | transfert=" + log.getTransferId()
                        + " | action=" + safe(log.getAction() != null ? log.getAction().name() : null)
                        + " | " + safe(log.getPreviousStatus()) + " -> " + safe(log.getNewStatus())
                        + " | notes=" + safe(log.getNotes()))
                .collect(Collectors.joining("\n"));
    }

    private String formatDonorAlertSamples(List<DonorAlert> donorAlerts) {
        if (donorAlerts.isEmpty()) {
            return "- Aucune réponse donneur";
        }
        return donorAlerts.stream()
                .sorted(Comparator.comparing(this::extractAlertTimestamp, this::compareTimestamps).reversed())
                .limit(SAMPLE_LIMIT)
                .map(alert -> "- " + safeTimestamp(extractAlertTimestamp(alert))
                        + " | alerte=" + safe(alert.getAlertId())
                        + " | donneur=" + safe(alert.getDonorId())
                        + " | réponse=" + safe(alert.getDonorResponse() != null ? alert.getDonorResponse().name() : null)
                        + " | lu=" + (alert.isRead() ? "oui" : "non"))
                .collect(Collectors.joining("\n"));
    }

    private Timestamp extractAlertTimestamp(DonorAlert alert) {
        if (alert == null) {
            return null;
        }
        return alert.getNotificationSentAt() != null ? alert.getNotificationSentAt() : alert.getReadAt();
    }

    private int compareTimestamps(Timestamp left, Timestamp right) {
        if (left == null && right == null) {
            return 0;
        }
        if (left == null) {
            return -1;
        }
        if (right == null) {
            return 1;
        }
        return left.compareTo(right);
    }

    private String safeTimestamp(Timestamp timestamp) {
        if (timestamp == null) {
            return "date inconnue";
        }
        return timestamp.toLocalDateTime().format(DATE_TIME_FORMATTER);
    }

    private String safe(String value) {
        if (value == null || value.isBlank()) {
            return "n/a";
        }
        return value.trim().replaceAll("\\s{2,}", " ");
    }

    private String formatAnalysisForDisplay(String rawText) {
        if (rawText == null || rawText.isBlank()) {
            return "";
        }

        String normalized = rawText
                .replace("\r\n", "\n")
                .replace('\r', '\n')
                .replace("**", "")
                .replace("__", "")
                .replace("•", "- ")
                .replaceAll("(?m)^\\s*[*]\\s+", "- ")
                .replaceAll("(?m)^\\s*[-]\\s*", "- ")
                .trim();

        normalized = normalized.replaceAll("(?i)résumé\\s*:", "Résumé :");
        normalized = normalized.replaceAll("(?i)signaux faibles\\s*:", "Signaux faibles :");
        normalized = normalized.replaceAll("(?i)risques\\s*:", "Risques :");
        normalized = normalized.replaceAll("(?i)recommandations\\s*:", "Recommandations :");

        normalized = normalized.replaceAll("(?i)(Résumé\\s*:)", "\n$1");
        normalized = normalized.replaceAll("(?i)(Signaux faibles\\s*:)", "\n$1");
        normalized = normalized.replaceAll("(?i)(Risques\\s*:)", "\n$1");
        normalized = normalized.replaceAll("(?i)(Recommandations\\s*:)", "\n$1");

        String[] lines = normalized.split("\\n+");
        StringBuilder builder = new StringBuilder();
        for (String line : lines) {
            String trimmed = line.trim();
            if (trimmed.isEmpty()) {
                continue;
            }

            if (isSectionHeading(trimmed)) {
                if (builder.length() > 0) {
                    builder.append("\n\n");
                }
                builder.append(trimmed);
                continue;
            }

            if (!trimmed.startsWith("- ")) {
                trimmed = "- " + trimmed.replaceAll("^[-–—]\\s*", "");
            }

            if (builder.length() > 0 && builder.charAt(builder.length() - 1) != '\n') {
                builder.append('\n');
            }
            builder.append(trimmed);
        }

        return builder.toString().trim();
    }

    private boolean isSectionHeading(String line) {
        String normalized = line.toLowerCase();
        return normalized.startsWith("résumé :")
                || normalized.startsWith("signaux faibles :")
                || normalized.startsWith("risques :")
                || normalized.startsWith("recommandations :")
                || normalized.startsWith("contexte :");
    }
}

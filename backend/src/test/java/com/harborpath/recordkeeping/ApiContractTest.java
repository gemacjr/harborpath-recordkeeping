package com.harborpath.recordkeeping;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers(disabledWithoutDocker = true)
class ApiContractTest {

	@Container
	@ServiceConnection
	static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

	@Autowired
	MockMvc mockMvc;

	@Autowired
	ObjectMapper objectMapper;

	@Test
	void loginSuccessReturnsJwtAndRole() throws Exception {
		mockMvc.perform(post("/api/v1/auth/login")
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{"username":"maya.chen","password":"harborpath"}
						"""))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.accessToken").isNotEmpty())
				.andExpect(jsonPath("$.tokenType").value("Bearer"))
				.andExpect(jsonPath("$.role").value("PARTICIPANT"))
				.andExpect(jsonPath("$.displayName").value("Maya Chen"));
	}

	@Test
	void loginFailureReturns401() throws Exception {
		mockMvc.perform(post("/api/v1/auth/login")
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{"username":"maya.chen","password":"wrong"}
						"""))
				.andExpect(status().isUnauthorized());
	}

	@Test
	void participantCannotReadCensus() throws Exception {
		String token = login("maya.chen", "harborpath");
		mockMvc.perform(get("/api/v1/plans/" + SeedIds.PLAN + "/census")
				.header("Authorization", "Bearer " + token))
				.andExpect(status().isForbidden())
				.andExpect(jsonPath("$.error").value("This chair isn’t yours."));
	}

	@Test
	void sponsorCannotPatchDeferral() throws Exception {
		String token = login("jordan.ellis", "harborpath");
		mockMvc.perform(patch("/api/v1/accounts/" + SeedIds.MAYA_401K + "/deferral")
				.header("Authorization", "Bearer " + token)
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{"percent":8}
						"""))
				.andExpect(status().isForbidden());
	}

	@Test
	void deferralAboveCapReturns422() throws Exception {
		String token = login("maya.chen", "harborpath");
		mockMvc.perform(patch("/api/v1/accounts/" + SeedIds.MAYA_401K + "/deferral")
				.header("Authorization", "Bearer " + token)
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{"percent":51}
						"""))
				.andExpect(status().isUnprocessableEntity())
				.andExpect(jsonPath("$[0].field").value("percent"));
	}

	@Test
	void deferralAtCapSucceeds() throws Exception {
		String token = login("maya.chen", "harborpath");
		mockMvc.perform(patch("/api/v1/accounts/" + SeedIds.MAYA_401K + "/deferral")
				.header("Authorization", "Bearer " + token)
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{"percent":50}
						"""))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.deferralPercent").value(50.0));
	}

	@Test
	void deferralPersistsIntoCensus() throws Exception {
		String maya = login("maya.chen", "harborpath");
		mockMvc.perform(patch("/api/v1/accounts/" + SeedIds.MAYA_401K + "/deferral")
				.header("Authorization", "Bearer " + maya)
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{"percent":8}
						"""))
				.andExpect(status().isOk());

		String jordan = login("jordan.ellis", "harborpath");
		MvcResult result = mockMvc.perform(get("/api/v1/plans/" + SeedIds.PLAN + "/census")
				.header("Authorization", "Bearer " + jordan))
				.andExpect(status().isOk())
				.andReturn();

		JsonNode census = objectMapper.readTree(result.getResponse().getContentAsString());
		JsonNode mayaRow = null;
		for (JsonNode row : census) {
			if (SeedIds.MAYA_PARTICIPANT.toString().equals(row.path("participantId").asText())) {
				mayaRow = row;
			}
		}
		assertThat(mayaRow).isNotNull();
		assertThat(mayaRow.path("deferralPercent").decimalValue().doubleValue()).isEqualTo(8.0);
	}

	@Test
	void participantCannotPostPayroll() throws Exception {
		String token = login("maya.chen", "harborpath");
		mockMvc.perform(post("/api/v1/plans/" + SeedIds.PLAN + "/payroll")
				.header("Authorization", "Bearer " + token)
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{"periodPay":5000}
						"""))
				.andExpect(status().isForbidden());
	}

	@Test
	void payrollPostsUsingCurrentDeferralAndShowsOnLedger() throws Exception {
		String maya = login("maya.chen", "harborpath");
		mockMvc.perform(patch("/api/v1/accounts/" + SeedIds.MAYA_401K + "/deferral")
				.header("Authorization", "Bearer " + maya)
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{"percent":8}
						"""))
				.andExpect(status().isOk());

		String jordan = login("jordan.ellis", "harborpath");
		mockMvc.perform(post("/api/v1/plans/" + SeedIds.PLAN + "/payroll")
				.header("Authorization", "Bearer " + jordan)
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{"periodPay":5000}
						"""))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.accountsPosted").value(2));

		MvcResult household = mockMvc.perform(get("/api/v1/households/me")
				.header("Authorization", "Bearer " + maya))
				.andExpect(status().isOk())
				.andReturn();
		JsonNode tape = objectMapper.readTree(household.getResponse().getContentAsString()).path("tape");
		assertThat(tape.path("ytdEmployee").decimalValue().doubleValue()).isEqualTo(8800.0);

		mockMvc.perform(get("/api/v1/contributions/me")
				.header("Authorization", "Bearer " + maya))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$[0].kind").exists());
	}

	@Test
	void educationContributionAndOverdraftWithdrawal() throws Exception {
		String maya = login("maya.chen", "harborpath");
		mockMvc.perform(post("/api/v1/accounts/" + SeedIds.MAYA_529 + "/contributions")
				.header("Authorization", "Bearer " + maya)
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{"amount":250}
						"""))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.kind").value("EDUCATION_529"));

		mockMvc.perform(post("/api/v1/accounts/" + SeedIds.MAYA_529 + "/withdrawals")
				.header("Authorization", "Bearer " + maya)
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{"amount":999999,"memo":"tuition"}
						"""))
				.andExpect(status().isUnprocessableEntity())
				.andExpect(jsonPath("$[0].field").value("amount"));
	}

	@Test
	void loanOn529AndOverCapAreRejected() throws Exception {
		String maya = login("maya.chen", "harborpath");
		mockMvc.perform(post("/api/v1/accounts/" + SeedIds.MAYA_529 + "/loans")
				.header("Authorization", "Bearer " + maya)
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{"amount":5000}
						"""))
				.andExpect(status().isUnprocessableEntity())
				.andExpect(jsonPath("$[0].field").value("amount"));

		mockMvc.perform(post("/api/v1/accounts/" + SeedIds.MAYA_401K + "/loans")
				.header("Authorization", "Bearer " + maya)
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{"amount":50000.01}
						"""))
				.andExpect(status().isUnprocessableEntity())
				.andExpect(jsonPath("$[0].field").value("amount"));
	}

	@Test
	void sponsorCannotRequestLoanAndParticipantCannotApprove() throws Exception {
		String jordan = login("jordan.ellis", "harborpath");
		mockMvc.perform(post("/api/v1/accounts/" + SeedIds.MAYA_401K + "/loans")
				.header("Authorization", "Bearer " + jordan)
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{"amount":5000}
						"""))
				.andExpect(status().isForbidden());

		String maya = login("maya.chen", "harborpath");
		MvcResult created = mockMvc.perform(post("/api/v1/accounts/" + SeedIds.MAYA_401K + "/loans")
				.header("Authorization", "Bearer " + maya)
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{"amount":5000}
						"""))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.status").value("REQUESTED"))
				.andReturn();
		String loanId = objectMapper.readTree(created.getResponse().getContentAsString()).get("id").asText();

		mockMvc.perform(patch("/api/v1/loans/" + loanId)
				.header("Authorization", "Bearer " + maya)
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{"decision":"APPROVE"}
						"""))
				.andExpect(status().isForbidden());

		mockMvc.perform(patch("/api/v1/loans/" + loanId)
				.header("Authorization", "Bearer " + jordan)
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{"decision":"DENY"}
						"""))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.status").value("DENIED"));
	}

	@Test
	void approvedLoanDebits401kAndPostsLedgerDisbursement() throws Exception {
		String maya = login("maya.chen", "harborpath");
		MvcResult householdBefore = mockMvc.perform(get("/api/v1/households/me")
				.header("Authorization", "Bearer " + maya))
				.andExpect(status().isOk())
				.andReturn();
		JsonNode beforeAccounts = objectMapper.readTree(householdBefore.getResponse().getContentAsString())
				.path("accounts");
		double before = 0;
		for (JsonNode account : beforeAccounts) {
			if (SeedIds.MAYA_401K.toString().equals(account.path("id").asText())) {
				before = account.path("balance").decimalValue().doubleValue();
			}
		}

		MvcResult created = mockMvc.perform(post("/api/v1/accounts/" + SeedIds.MAYA_401K + "/loans")
				.header("Authorization", "Bearer " + maya)
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{"amount":5000}
						"""))
				.andExpect(status().isOk())
				.andReturn();
		String loanId = objectMapper.readTree(created.getResponse().getContentAsString()).get("id").asText();

		String jordan = login("jordan.ellis", "harborpath");
		mockMvc.perform(patch("/api/v1/loans/" + loanId)
				.header("Authorization", "Bearer " + jordan)
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{"decision":"APPROVE"}
						"""))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.status").value("DISBURSED"));

		MvcResult householdAfter = mockMvc.perform(get("/api/v1/households/me")
				.header("Authorization", "Bearer " + maya))
				.andExpect(status().isOk())
				.andReturn();
		JsonNode accounts = objectMapper.readTree(householdAfter.getResponse().getContentAsString()).path("accounts");
		JsonNode retirement = null;
		for (JsonNode account : accounts) {
			if (SeedIds.MAYA_401K.toString().equals(account.path("id").asText())) {
				retirement = account;
			}
		}
		assertThat(retirement.path("balance").decimalValue().doubleValue()).isEqualTo(before - 5000.0);

		mockMvc.perform(get("/api/v1/contributions/me")
				.header("Authorization", "Bearer " + maya))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$[0].kind").value("LOAN_DISBURSEMENT"))
				.andExpect(jsonPath("$[0].amount").value(-5000.0));
	}

	@Test
	void clearingPrimaryOpensComputedAlertAndCensusMarksIncomplete() throws Exception {
		String maya = login("maya.chen", "harborpath");
		mockMvc.perform(put("/api/v1/accounts/" + SeedIds.MAYA_401K + "/beneficiaries")
				.header("Authorization", "Bearer " + maya)
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						[{"fullName":"Alex Chen","relationship":"Spouse","percent":100,"primaryFlag":false}]
						"""))
				.andExpect(status().isOk());

		String jordan = login("jordan.ellis", "harborpath");
		MvcResult alerts = mockMvc.perform(get("/api/v1/plans/" + SeedIds.PLAN + "/alerts")
				.header("Authorization", "Bearer " + jordan))
				.andExpect(status().isOk())
				.andReturn();
		JsonNode queue = objectMapper.readTree(alerts.getResponse().getContentAsString());
		JsonNode mayaOpen = null;
		for (JsonNode alert : queue) {
			if (SeedIds.MAYA_PARTICIPANT.toString().equals(alert.path("participantId").asText())
					&& "MISSING_PRIMARY_BENEFICIARY".equals(alert.path("code").asText())
					&& "OPEN".equals(alert.path("status").asText())) {
				mayaOpen = alert;
			}
		}
		assertThat(mayaOpen).isNotNull();

		MvcResult census = mockMvc.perform(get("/api/v1/plans/" + SeedIds.PLAN + "/census")
				.header("Authorization", "Bearer " + jordan))
				.andExpect(status().isOk())
				.andReturn();
		JsonNode mayaRow = censusRow(census, SeedIds.MAYA_PARTICIPANT.toString());
		assertThat(mayaRow.path("beneficiaryComplete").asBoolean()).isFalse();

		mockMvc.perform(put("/api/v1/accounts/" + SeedIds.MAYA_401K + "/beneficiaries")
				.header("Authorization", "Bearer " + maya)
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						[{"fullName":"Alex Chen","relationship":"Spouse","percent":100,"primaryFlag":true}]
						"""))
				.andExpect(status().isOk());
	}

	@Test
	void restoringPrimaryClearsComputedAlert() throws Exception {
		String maya = login("maya.chen", "harborpath");
		mockMvc.perform(put("/api/v1/accounts/" + SeedIds.MAYA_401K + "/beneficiaries")
				.header("Authorization", "Bearer " + maya)
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						[{"fullName":"Alex Chen","relationship":"Spouse","percent":100,"primaryFlag":false}]
						"""))
				.andExpect(status().isOk());
		mockMvc.perform(put("/api/v1/accounts/" + SeedIds.MAYA_401K + "/beneficiaries")
				.header("Authorization", "Bearer " + maya)
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						[{"fullName":"Alex Chen","relationship":"Spouse","percent":100,"primaryFlag":true}]
						"""))
				.andExpect(status().isOk());

		String jordan = login("jordan.ellis", "harborpath");
		MvcResult alerts = mockMvc.perform(get("/api/v1/plans/" + SeedIds.PLAN + "/alerts")
				.header("Authorization", "Bearer " + jordan))
				.andExpect(status().isOk())
				.andReturn();
		JsonNode queue = objectMapper.readTree(alerts.getResponse().getContentAsString());
		JsonNode mayaAlert = null;
		for (JsonNode alert : queue) {
			if (SeedIds.MAYA_PARTICIPANT.toString().equals(alert.path("participantId").asText())
					&& "MISSING_PRIMARY_BENEFICIARY".equals(alert.path("code").asText())) {
				mayaAlert = alert;
			}
		}
		assertThat(mayaAlert).isNotNull();
		assertThat(mayaAlert.path("status").asText()).isEqualTo("CLEARED");

		JsonNode priya = null;
		for (JsonNode alert : queue) {
			if (SeedIds.PRIYA_PARTICIPANT.toString().equals(alert.path("participantId").asText())) {
				priya = alert;
			}
		}
		assertThat(priya.path("status").asText()).isEqualTo("OPEN");
	}

	@Test
	void deferralChangeAppearsOnPlanAuditAndParticipantCannotReadIt() throws Exception {
		String maya = login("maya.chen", "harborpath");
		mockMvc.perform(patch("/api/v1/accounts/" + SeedIds.MAYA_401K + "/deferral")
				.header("Authorization", "Bearer " + maya)
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{"percent":9}
						"""))
				.andExpect(status().isOk());

		mockMvc.perform(get("/api/v1/plans/" + SeedIds.PLAN + "/audit")
				.header("Authorization", "Bearer " + maya))
				.andExpect(status().isForbidden());

		String jordan = login("jordan.ellis", "harborpath");
		MvcResult audit = mockMvc.perform(get("/api/v1/plans/" + SeedIds.PLAN + "/audit")
				.header("Authorization", "Bearer " + jordan))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$[0].action").value("DEFERRAL_CHANGED"))
				.andExpect(jsonPath("$[0].actorUsername").value("maya.chen"))
				.andReturn();
		JsonNode first = objectMapper.readTree(audit.getResponse().getContentAsString()).get(0);
		assertThat(first.path("summary").asText()).contains("9");
	}

	private JsonNode censusRow(MvcResult census, String participantId) throws Exception {
		JsonNode rows = objectMapper.readTree(census.getResponse().getContentAsString());
		for (JsonNode row : rows) {
			if (participantId.equals(row.path("participantId").asText())) {
				return row;
			}
		}
		return null;
	}

	private String login(String username, String password) throws Exception {
		MvcResult result = mockMvc.perform(post("/api/v1/auth/login")
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{"username":"%s","password":"%s"}
						""".formatted(username, password)))
				.andExpect(status().isOk())
				.andReturn();
		return objectMapper.readTree(result.getResponse().getContentAsString()).get("accessToken").asText();
	}
}

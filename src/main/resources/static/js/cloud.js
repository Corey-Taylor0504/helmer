function getAddress() {
    $("#address").empty();
    var address = $("#address");
    var project = $("#project").find(":selected").val();
    var env = $("#env").find(":selected").val();
	$.ajax({
        url: "api/ui/rest/getAddress?project="+project+"&env="+env,
	success : function(result, status) {
		$.each(result, function(item) {
                address.append($("<option />").val(result[item]).text(result[item]));
            });
          if(project=="GKE-AUTOPILOT"){
          $('#cli').val('true');
          $('#cli').prop('disabled', 'disabled');
          }else{
           $('#cli').prop('disabled', false);
          }
	}
    });
}

function loadAddress(firstProject) {
    $("#address").empty();
    var address = $("#address");
    var env = $("#env").find(":selected").val();
	$.ajax({
        url: "api/ui/rest/getAddress?project="+firstProject+"&env="+env,
	success : function(result, status) {
		$.each(result, function(item) {
                address.append($("<option />").val(result[item]).text(result[item]));
            });
	}
    });
}
function getProject() {
    var firstProject;
    $("#project").empty();
    var projects = $("#project");
    var toolTip = "Please select 'pr-prisma-cloud-defender.homedepot.com' or 'np--prisma-cloud-defender.homedepot.com' for any Cloud workload - GKE, AKS, EKS etc. For Kosmos clusters select 'pr-prisma-internal-defender.homedepot.com' or 'np-prisma-internal-defender.homedepot.com' based on enviornment pr-* is Production np-* is Non-Production/LLC";
 	$.ajax({
        url: "api/ui/rest/getProject",
	success : function(result, status) {
    $.each(result, function(item) {
            projects.append($("<option />").val(result[item]).text(result[item]).attr({"title":toolTip}));
        });
      loadAddress(result[0]);
	}
 });
}

function validateFields() {
   var searchFlag = $("#searchFlag").val();
    var projectId = $("#projectId").val();
    if(projectId==""){
    alert("please enter projectId");
    return false;
    }
    if(searchFlag!="true"){
          var experience = $("#experience").val();
           if(experience==""){
           alert("please enter sub experience");
           return false;
           }
           var owner = $("#owner").val();
           if(owner==""){
           alert("please enter sub owner");
           return false;
           }
    }
    var cluster = $("#cluster").val();
    if(cluster==""){
    alert("please enter cluster");
    return false;
    }
    return true;
}

function ssoLogin(){
    window.open("saml/login","_self");
}

function goBack(){
    window.open("logout","_self");
}

function searchCluster() {
$('#searchFlag').val("true");
$("#cloudForm").submit();
}

function deleteCluster() {
var clusterName = $("#clusterHiddenId").val();
var projectId = $("#gcpId").val();
window.open("delete?gcpProjectId="+projectId+"&clusterId="+clusterName,"_self");
}

function upgradeCluster() {
var clusterName = $("#clusterHiddenId").val();
var projectId = $("#gcpId").val();
window.open("upgradeCluster?gcpProjectId="+projectId+"&clusterId="+clusterName,"_self");
}
using AuditHub.Config;
using AuditHub.Services;

var builder = WebApplication.CreateBuilder(args);

builder.Services.AddControllersWithViews();
builder.Services.Configure<JiraSettings>(builder.Configuration.GetSection("Jira"));
builder.Services.AddHttpClient<JiraService>();
builder.Services.AddSingleton<DashboardService>();
builder.Services.AddSingleton<JqlBuilderService>();
builder.Services.AddSingleton<ExcelService>();

var app = builder.Build();

if (!app.Environment.IsDevelopment())
{
    app.UseExceptionHandler("/Home/Error");
}

app.UseStaticFiles();
app.UseRouting();

app.MapControllerRoute(
    name: "default",
    pattern: "{controller=Home}/{action=Index}/{id?}");

app.Run();
